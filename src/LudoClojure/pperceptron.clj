(ns LudoClojure.pperceptron
 ; (:use [LudoClojure.spindle])
 ; (:use [LudoClojure.spinopencl])
  (:use [LudoClojure.opencl-utils])
  )
(use 'calx)

(+ 1 2)
(println "loading pperceptron")


;TODO define what we want
;Implement it

;; PP Naming convention
;;   pps               is an array of pp (parallel-perceptrons) (not being impelented in the early stage, but this is the target)
;;   pp                is one paralel-perceptron
;;   perceptron        is one of the perceptrons within a pp
;;   alpha             holds over all pps, each pp and each perceptron, the weight poiting to input_data array
;;   input_data        data being observed by the system, this is 'z' in the literature.
;;   vecProductResult  holds alpha*input_data per perceptron
;;   pp_answer         Result of pps. The results of each pp. Each array element is a result of a pp.
;;   correct_answer    What pp_answer should be.


;;       const int   input_size,    // size of inputs plus bias term
;;       const float eta,           // learning rate
;;       const float gama,          // margin around zero
;;       const float epsilon,       // level of error that is allowed
;;       const float mu,            // learning modifier around zero



(add_kernel_specs opencl_kernels
{
:vecProduct {
     :desc "computes a vector product of an *in sized array by *in times *out  
vector, resultingin *out sized array, notes on approach here:
http://www.bealto.com/gpu-gemv_v1.html"
     :postion 10
     :body "
__kernel void vecProduct(
    const int input_size,
    __global float *input_data,
    __global float *alpha,
    __global float *vecProductResult
    )
{

float total = 0.0;
int gid = get_global_id(0);

  for(int k=0; k<input_size; k++) {
       total += alpha[gid * input_size + k] * input_data[k];
  }
vecProductResult[gid] = total;
}

  "
}
:reduceToPP {
     :desc "reduces the many perceptrons outputs to singular pp output"
     :postion 10
     :body "
__kernel void reduceToPP(
    const int pp_size,
    const int rho,
    __global float *vecProductResult,
    __global float *pp_answer_buf
    )
{

float total = 0.0;
float binaryPPout = 0.0;

int gid = get_global_id(0);

  for(int k=0; k<pp_size; k++) {
      
     binaryPPout = vecProductResult[gid * pp_size + k];
        if( binaryPPout >= 0.0) {
            total += 1.0; 
           }
        else {
            total += -1.0;
             }
  }
//OLD Implementatioon    pp_answer_buf[gid] = total / pp_size;  //Different squashing functions could be implemente here, eg: binary 1,-1   devide by pp_size is actually incorrect.
// TODO rho goes here
if (total < -rho) {  
    pp_answer_buf[gid] = -1.0;
    }
else if (total > rho) {  
    pp_answer_buf[gid] = +1.0;
    }
else {
    pp_answer_buf[gid] = total / rho ;
   }

}
"
}


:updateAlphas {
     :desc "updates the alphas based on how correct the previous pp was.   
TODO: optimisation: mearge this kernel with that of vecProduct so that we don't have to read alphas twice and call two kernels "
     :postion 10
     :body "
__kernel void updateAlphas(
       const int   number_of_pps,
       const int   pp_size,
       const int   input_size,    //size of inputs plus bias term
       const float eta,           // learning rate
       const float gama,          // margin around zero
       const float epsilon,       // level of error that is allowed
       const float mu,            //learning modifier around zero
    __global float *input_data,
    __global float *vecProductResult,
    __global float *pp_answer_buf,
    __global float *correct_answer_buf,
    __global float *alpha
    )
{

float total = 0.0;
int gid = get_global_id(0);



// which perceptron are we talking to
int perceptronid = gid * input_size;

//which pp are we taking pp_answer_buf and correct_answer_buf for?
//TODO check this division behaves as needed here, does it round down?
int pp_we_are_talking_to = gid / pp_size;


float pp_answer      =      pp_answer_buf[pp_we_are_talking_to];
float upper_correct  = correct_answer_buf[pp_we_are_talking_to] + epsilon;
float lower_correct  = correct_answer_buf[pp_we_are_talking_to] - epsilon;
float vProductResult = vecProductResult[gid];

//WIP computing the norm of the alphas for this perceptron
float alphapart            = 0.0;
float alpha_norm_squared   = 0.0;
for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha_norm_squared +=  (alphapart * alphapart);
       }
float alpha_norm_squared_adjustment  = (alpha_norm_squared - 1.0) * eta;      //



if ((pp_answer > upper_correct) && (vProductResult >= 0.0)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha[perceptronid + k] =  alphapart - (alpha_norm_squared_adjustment * alphapart) - (input_data[k] * eta);
       }
}
else if ((pp_answer < lower_correct) && (vProductResult < 0.0)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha[perceptronid + k] =  alphapart - (alpha_norm_squared_adjustment * alphapart) + (input_data[k] * eta);
       }
}
//Here we Stabilizing the outputs around zero
else if ((pp_answer <= upper_correct) && (0.0 <= vProductResult) && (vProductResult < gama)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha[perceptronid + k] =  alphapart - (alpha_norm_squared_adjustment * alphapart) + (input_data[k] * eta * mu);
       }
}
else if ((pp_answer >= lower_correct) && (vProductResult < 0)    && (-gama < vProductResult)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha[perceptronid + k] =  alphapart - (alpha_norm_squared_adjustment * alphapart) - (input_data[k] * eta * mu);
       }
}
else {
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha[perceptronid + k] =  alphapart - (alpha_norm_squared_adjustment * alphapart);
       }
}
}
"
}})

;;Compile openCL kernels into the opencl environment
(opencl_env_compileprogs opencl_env (get_openCL_from_kernels opencl_kernels))


(defn make_pp ;[opencl_env  input_size  outputs_size  pp_size rho]
               [options]
  "TODO use destructuring to pass in a map that configures the pp, provide defaults"
  (let [{:keys [pp_opencl_env input_size outputs_size pp_size rho eta gama epsilon mu rho] 
                 :or 
                {pp_opencl_env opencl_env 
                 input_size 3
                 outputs_size 1
                 pp_size 3
                 rho 1
                 eta (float 0.001)
                 gama (float 0.4)
                 epsilon (float 0.03)
                 mu (float 0.9 )}} options
        size_of_alpha_needed (* input_size pp_size outputs_size)
        input_data_buf       (lg_create-buffer (:context @opencl_env) input_size :float32-le)
        alpha_buf            (lg_wrap (:context @opencl_env) (make_random_float_array size_of_alpha_needed -0.5 1) :float32-le)
        vecProductResult_buf (lg_create-buffer (:context @opencl_env) pp_size :float32-le)
        correct_answer_buf   (lg_create-buffer (:context @opencl_env) outputs_size :float32-le)
        pp_answer_buf        (lg_create-buffer (:context @opencl_env) outputs_size :float32-le)
        ]

  {:alpha_buf            alpha_buf
   :input_data_buf       input_data_buf
   :vecProductResult_buf vecProductResult_buf
   :correct_answer_buf   correct_answer_buf
   :pp_answer_buf pp_answer_buf
   :opencl_env opencl_env
   
   :input_size input_size
   :outputs_size outputs_size
   :pp_size pp_size
   
   :rho           rho           ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
   :eta           eta           ;;  learning_rate
   :gama          gama          ;;  margin around zero
   :epsilon       epsilon       ;;  level of error that is allowed.
   :mu            mu            ;;  learning modifier around zero
   }
  ))

(defn pp_vecproduct [pp]
  "Part one stage of compoutig a pp answer. Takes input data that and alphas and does a vector product over them"
  (lg_enqueue-kernel (:queue @(:opencl_env pp)) (:progs @(:opencl_env pp))
      :vecProduct  (* (:pp_size pp) (:outputs_size pp))    ;;global size, here number of perceptrons in the one pp
                          (:input_size pp)  ;;inluding the bias term
                          (:input_data_buf pp)
                          (:alpha_buf pp)    ;;should be 
                          (:vecProductResult_buf pp)))


(defn reduceToPP [pp]
  "Part two takes the vector products result and adds it up for each pp, giving an integer which is the pp answer, this is scaled 
down to between -1.0 and 1.0"
  (lg_enqueue-kernel (:queue @(:opencl_env pp)) (:progs @(:opencl_env pp))
                          :reduceToPP
                          (:outputs_size pp) ;;global size, here total number of perceptrons, here just one
                          (:pp_size pp)
                          (:rho  pp)
                          (:vecProductResult_buf pp)
                          (:pp_answer_buf pp)))


(defn flop_pp [pp]
 "The 3rd stage does the learning by updating the alphas vector by considering the correct answer provided and the answer given by the current pp"
  (lg_enqueue-kernel (:queue @(:opencl_env pp)) (:progs @(:opencl_env pp))
                          :updateAlphas
                          (* (:pp_size pp) (:outputs_size pp))        ;global size, here number of perceptrons in the one pp * number_of_pps
                          (:outputs_size pp)     ; we will need to bring in global data relavant to the pp to each perceptron within, a kind of join, 
                          (:pp_size pp)
                          (:input_size pp)          ;will be looping using this parameter to update all alphas
                          (:eta pp)
                          (:gama pp)
                          (:epsilon pp)
                          (:mu pp)
                          (:input_data_buf pp)
                          (:vecProductResult_buf pp)
                          (:pp_answer_buf pp)
                          (:correct_answer_buf pp)
                          (:alpha_buf pp)
                              ;;Strategy is compute all global values first, then loop over the alpha buffer to apply updates if needed
                          ))

(defn pp_write_input [pp data_float_vec ]
  ;;TODO add check for size to be as expects
       (lg_enqueue-overwrite (:input_data_buf pp) [(- (:input_size pp)) 0] (to-buffer data_float_vec :float32-le) (:queue @(:opencl_env pp)))
  ;;TODO return an event
  )

(defn pp_write_correct_answer [pp data_float_vec ]
  ;;TODO add check for size to be as expects
       (lg_enqueue-overwrite (:correct_answer_buf pp) [(- (:outputs_size pp)) 0] (to-buffer data_float_vec :float32-le) (:queue @(:opencl_env pp)))
  ;;TODO return an event
  )



(quote do stuff to a pp


(def pp1 (make_pp {:input_size 10
                   :outputs_size 21
                   :pp_size 30
                   :rho 20                   ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.0001)       ;;  learning_rate
                   :gama (float 0.4)        ;;  margin around zero              ;0.4
                   :epsilon (float 0.01)    ;;  level of error that is allowed.
                   :mu (float 0.9 )}))      ;;  learning modifier around zero   ;0.9

@(lg_enqueue-read (pp1 :input_data_buf) (:queue @opencl_env))
@(lg_enqueue-read (pp1 :correct_answer_buf) (:queue @opencl_env))
@(lg_enqueue-read (pp1 :pp_answer_buf) (:queue @opencl_env))
;  @(lg_enqueue-read (pp1 :vecProductResult_buf) (:queue @opencl_env))
;  @(lg_enqueue-read (pp1 :alpha_buf) (:queue @opencl_env))

(pp_write_input pp1 [1.0 -1.0 -0.1 1.0 -1.0])
(pp_write_correct_answer pp1 [-1.0 -0.9 -0.8 -0.7 -0.6 -0.5 -0.4 -0.3 -0.2 -0.1 0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0])
;(pp_write_correct_answer pp1 [1.0 0.9 0.8 0.7 0.6 0.5 0.4 0.3 0.2 0.1 0.0 -0.1 -0.2 -0.3 -0.4 -0.5 -0.6 -0.7 -0.8 -0.9 -1.0])
;(pp_write_correct_answer pp1 [-0.7 0.512 -1.0 1.0 -1.0 1.0 -1.0 1.0 -1.0 1.0 -1.0 1.0 -1.0 1.0 -1.0 1.0 -1.0 1.0 -1.0 1.0 -1.0])


(time (dotimes [n 200] 
(pp_vecproduct pp1)
(reduceToPP pp1)
(flop_pp pp1)
(if (= 0 (mod n 1))
(println n @(lg_enqueue-read (pp1 :pp_answer_buf) (:queue @opencl_env))  ); @(lg_enqueue-read (pp1 :alpha_buf) (:queue @opencl_env)) )
)
;(println @(lg_enqueue-read (pp1 :pp_answer_buf) (:queue @opencl_env))  @(lg_enqueue-read (pp1 :alpha_buf) (:queue @opencl_env)) )
))
;[-1.0 -0.093 -1.0]
;[-1.0 1.0 -1.0]
;[-1.0 -0.101 -1.0]
;[-1.0 1.0 -1.0]

(lg_finish (:queue @opencl_env))



)

(quote
  TODO
  API functions to wrap over pp

  
  (defn make_pp [opencl_env input_size outputs_size pp_size] {...buffers (atom of configs)})
        ;; sets up all buffers, default config)
  (defn update_pp_config [pp {of options }] (swap! pp ..))   ;; to update learning rates etc...)  ;; meta learning functionality here, eg: to optimise learning rate etc?
  (defn train_pp!   (pp in_vec correct_answer_vec))   ;my need to expose openCL queue ; my wish to pass in buffers for speed, ie: have fast version of these where queue and nonCL buffers are passed in,.. or even target openCL buffers (comes in under openCL interoprility) 
  (defn readout_pp! (pp in_vec answer_vec))
  (defn train_examples [pp, traincycles, vec of in_vecs, vec of correct_answer_vecs] trains pp for n staps, emit assesment of accuracy)
  
  ;;;;;TODO
  ;;Make these functions of pp being made, eg: ((pp :train) [vecin] [vecout])  ...rifle style
  ;;This removes the need for the pp to be passed in each time for helper function definitions... object is the persistant clojure...
  
  
  ;;??!! What should the returns be for these... the pp, should be cheap to... sideeffect rampany however...?
  ;;correct clFinish in to be done by user 
  
  ;;assume pp is first of a famility of interoperabable openCL entities, need to set up prototype intefaces between them.
  ;will need to talk at openCL buffer level if possible (eg: one pp passing value to another, liquid to pp passing)... aim for aritrary graph between openCL entities
  ;ie: have funs that take a pp and hook in input from other openCL object, amit to other openCL object....
  
  ;;Learn defprotocol, defrecord, defmethod etc.... try to use them here...
    )
  
  ;;Assume a single openCL env? Needs to know sizes of pp


;opencl_env
;testing kernel compilation 




(quote

;see which kernel programs
(keys (:progs @opencl_env))


;(def my_openCL_buf4  (lg_create-buffer my_context 10 :int32-le))
;(lg_wrap my_context [5.0 5.2 5.3] :float32-le)






(def input_data_buf_atom   (atom (lg_wrap (:context @opencl_env) [1.0 0.0 -1.0] :int32-le)))
(def pp_size 100)
(def input_size (buf_elements @input_data_buf_atom))
(def size_of_alpha_needed (* input_size pp_size))

(time (def alpha  (let [buf (lg_wrap (:context @opencl_env) (make_random_float_array size_of_alpha_needed -0.5 1) :float32)]
                       (lg_finish (:queue @opencl_env))
                       buf
                       )
        ))

;;Demoing readig data off
(time (def readalpha @(lg_enqueue-read alpha (:queue @opencl_env))))
(count readalpha)

;;Demoing swapping in data via atomic swap
@(lg_enqueue-read @input_data_buf_atom (:queue @opencl_env))
(swap! input_data_buf_atom (fn [_] (lg_wrap (:context @opencl_env) [1.0 1.0 -1.0] :float32-le)))
@(lg_enqueue-read @input_data_buf_atom (:queue @opencl_env))


;;TODO, pull all these on a single pp object that has a generator?
(def vecProductResult (lg_create-buffer (:context @opencl_env) pp_size :float32-le))
(def correct_answer_buf (atom (lg_wrap (:context @opencl_env) [1.0] :float32-le)))
(def pp_answer_buf (lg_create-buffer (:context @opencl_env) 1 :float32-le))

(def debug_buff_int (lg_create-buffer (:context @opencl_env) 30 :int32-le))


(def number_of_pps (buf_elements @correct_answer_buf))









(time (lg_finish (:queue @opencl_env)))

(defn vecProduct []
  (lg_enqueue-kernel (:queue @opencl_env) (:progs @opencl_env)
      :vecProduct  (* pp_size number_of_pps)    ;;global size, here number of perceptrons in the one pp
                          input_size  ;;inluding the bias term
                          @input_data_buf_atom
                          alpha    ;;should be 
                          vecProductResult))

@(lg_enqueue-read vecProductResult (:queue @opencl_env))
(vecProduct)
@(lg_enqueue-read vecProductResult (:queue @opencl_env))



(defn reduceToPP []
(lg_enqueue-kernel (:queue @opencl_env) (:progs @opencl_env) 
                          :reduceToPP
                          number_of_pps ;;global size, here total number of perceptrons, here just one
                          pp_size
                          10 ;rho
                          vecProductResult
                          pp_answer_buf))
(defn reduceToPPq [q]
(lg_enqueue-kernel q (:progs @opencl_env) 
                          :reduceToPP
                          number_of_pps ;;global size, here total number of perceptrons, here just one
                          pp_size
                          10 ;rho
                          vecProductResult
                          pp_answer_buf))

@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))
(reduceToPP)
@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))



(def eta           (float 0.01)) ;;  learning_rate
(def gama          (float 0.05)) ;;  margin around zero
(def epsilon       (float 0.001)) ;;  level of error that is allowed.
(def mu            (float 0.5 )) ;;  learning modifier around zero


(quote
(defn flop_pp [] (lg_enqueue-kernel (:queue @opencl_env) (:progs @opencl_env) 
                          :updateAlphas
                          (* pp_size number_of_pps)        ;global size, here number of perceptrons in the one pp * number_of_pps
                          number_of_pps     ; we will need to bring in global data relavant to the pp to each perceptron within, a kind of join, 
                          pp_size
                          input_size          ;will be looping using this parameter to update all alphas
                          eta
                          gama
                          epsilon
                          mu
                          @input_data_buf_atom
                          vecProductResult
                          pp_answer_buf
                          @correct_answer_buf
                          alpha
                          debug_buff_int
                          ;;Strategy is compute all global values first, then loop over the alpha buffer to apply updates if needed
                          ))


(flop_pp) 

;;Target is, (flop_pp prior_event(can be nil for none) pp_object)   , emits  an event so that downstream can hook in to time order 

@(lg_enqueue-read alpha (:queue @opencl_env))

(opencl_env_addQueue opencl_env :queue2)

(def event1 (vecProduct))
(def event2 (reduceToPPq  (:queue @opencl_env)))

(def event22 (reduceToPPq  (:queue2 @opencl_env)))
(def event3 (flop_pp))

(status event1)
(status event2)
(status event22)
(status event3)

(lg_enqueue-wait-for (:queue @opencl_env) event1 )
(lg_enqueue-wait-for (:queue @opencl_env) event22 )
(lg_enqueue-wait-for (:queue @opencl_env) event3 )

(lg_enqueue-barrier (:queue @opencl_env))

(def markerevet1 (lg_enqueue-marker (:queue @opencl_env)))
(status markerevet1)
(wait-for markerevet1)

(time (lg_finish (:queue @opencl_env)))

;;; Set up a test showing that order of execution can be controlled with 
;lg_enqueue-wait-for specific events across queues, by making second queue to get 
;to a specific competion stage. Make it really easy to see the dependency graph.
;Could make the whole flop mechanisim be based on directed acyclic dependency graph





(vecProduct)
(reduceToPP)
(flop_pp)


@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))

@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))
@(lg_enqueue-read @correct_answer_buf (:queue @opencl_env))
(lg_enqueue-overwrite @correct_answer_buf [-1 0] (to-buffer [-1.0] :float32-le) (:queue @opencl_env))



(lg_enqueue-overwrite @input_data_buf_atom [-3 0] (to-buffer [1.0 0.0 -1.0] :float32-le) (:queue @opencl_env))
(lg_enqueue-overwrite @correct_answer_buf [-1 0] (to-buffer [-1.0] :float32-le) (:queue @opencl_env))
(vecProduct)
(reduceToPP)
@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))


(lg_enqueue-overwrite @input_data_buf_atom [-3 0] (to-buffer [1.0 1.0 -1.0] :float32-le) (:queue @opencl_env))
(lg_enqueue-overwrite @correct_answer_buf [-1 0] (to-buffer [1.0] :float32-le) (:queue @opencl_env))
(vecProduct)
(reduceToPP)
@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))


(time (def largearray (make_random_float_array 100 1.0 3)))  ;120ms
(time (def largebuff (lg_wrap (:context @opencl_env) largearray :float32-le)))   ;37ms
(time (def overwrtiewith  (to-buffer  largearray :float32-le)))     ;35ms
(time (lg_enqueue-overwrite largebuff [-100 0] overwrtiewith (:queue @opencl_env)))               ;2ms
(time (def mmarker (lg_enqueue-marker (:queue @opencl_env))))
(time (status mmarker))
(def readrandom (time @(lg_enqueue-read largebuff (:queue @opencl_env))))
(time (lg_finish (:queue @opencl_env)))
(first readrandom)



;;Demoing safe,closed buffer operations
(time 
  (let [buf1        (lg_wrap (:context @opencl_env) [2.0] :float32-le)
        local_queue (lg_create-queue (first (:devices @opencl_env)) (:context @opencl_env) )
        add_one_event  (lg_enqueue-kernel local_queue (:progs @opencl_env) :addOneToFloat 1 buf1 buf1)
        times_two_event (lg_enqueue-kernel local_queue (:progs @opencl_env) :timestwo 1 buf1 buf1)
        add_one_event2  (lg_enqueue-kernel local_queue (:progs @opencl_env) :addOneToFloat 1 buf1 buf1)
      ]
  
  @(lg_enqueue-read buf1 local_queue)
))




(time (do 1 2))
(def outtimecheck
  (let [buf1        (lg_wrap (:context @opencl_env) [2.0] :float32-le)
        local_queue (lg_create-queue (first (:devices @opencl_env)) (:context @opencl_env) )]
    
    (let [add_one_event  (lg_enqueue-kernel local_queue (:progs @opencl_env) :addOneToFloat 1 buf1 buf1)
                add_one_event2  (lg_enqueue-kernel local_queue (:progs @opencl_env) :addOneToFloat 1 buf1 buf1)
                times_two_event (lg_enqueue-kernel local_queue (:progs @opencl_env) :timestwo 1 buf1 buf1)]
         (time @(lg_enqueue-read buf1 local_queue)))
))


(opencl_env_compileprogs opencl_env (get_openCL_from_kernels opencl_kernels))

(time 
  (let [buf1         (lg_wrap (:context @opencl_env) [0.0] :float32-le)
        device       (first (:devices @opencl_env))
        local_queue  (lg_create-queue device (:context @opencl_env) )
        local_queue2 (lg_create-queue device (:context @opencl_env) )
        add_one_event  (lg_enqueue-kernel local_queue2 (:progs @opencl_env) :addOneToFloat 1 buf1 buf1)
        times_two_event (lg_enqueue-kernel local_queue (:progs @opencl_env) :timestwo 1 buf1 buf1)
        waitfor_event   (lg_enqueue-wait-for local_queue2 times_two_event )
        add_one_event2  (lg_enqueue-kernel local_queue2 (:progs @opencl_env) :addOneToFloat 1 buf1 buf1)

      ]
add_one_event2   ;;This is the point, without this wait we can read off witth add_one_event2 not being done yet
;@(lg_enqueue-read buf1 local_queue)
))

;;It's super cheep to create an openCL queue. 
(time (dotimes [n 100] 
   (lg_create-queue (first (:devices @opencl_env)) (:context @opencl_env) )))

;;;;; To wrap functionally, each set of actions needs to consume an array of openCL events to wait for
;;and emit one event which will signal the end of this functions work. Note that events will be enqued on openCL queues only, execution will be done later...
;Internall, each block mages whatever needs to happen (effectively hidden state), BUT
;need to make sure that buffers touched, ie: passed in are not touched anywhere else (this is a state leak out of the function, via the buffers being statefull)
;;need to be aware of which queue should have the wait-for event enqueued ....

;; how can one fan out?

;; The API (if it can be called that) will be based around each funcions that are made up potentailly of multiple openCL calls) having an interal queue.
;; The function will return a dependency event that WILL HAVE TO BE respected by callers.
;; Each function will accept an array of openCL events to depend on as first param + other params for itself.
;; A helper function to consume the array and depend on event within it will be provided, it will have to be impelmented by anyone wishing to use API
;; V2 Could have a macro to inject this depenency.

;eg
(defn do_pp [dep config]
    (let [local_q (lg_create-queue (first (:devices @opencl_env)) (:context @opencl_env) )
          deps    (lg_enqueue-wait-for local_q dep)]
          deps))
;;;TODO!! create support function that will return a queue . take an openCL_env and array(or singular or list) of dependency event, sets up them as dependencies on the queue
(do_pp (reduceToPP) 1)




(quote

(defn doallpp [dep]
  (let [d1  (do_pp dep config)
        d2  (postpp1 d1)
        d3  (portpp2 d1)
        d4  (do4 [d2 d3] config)]
   d4
  ))
;;call it with
    (doallpp (doallpp (doallpp nil)))
;;or use it among other dos for other entties

;;Note that this is where the high level design would live for composite structures
(defn doallstuff [dep]
  (let [ppdone     (doallpp dep pp)
        liqdobe    (doallliq dep liq)
        donecopy   (copy_pp_to_liq [ppdone liqdobe] pp liq copyconfig)]
     donecopy))

;;Then run with soething like

(loop [k 4 dep nil]
        (println k) (println dep)
        ;(doallstuff dep)
        (if (= k 1) 
            nil
            (recur (dec k)  (doallstuff dep))))

(defn returnlists [x y z]
   (list x y z))
(returnlists 1 2 '(1 2 3))

(map (fn [x] (+ x 1 )) (concat [3 2 1 0] [6 5 4] [8]))


)


)



)