(ns LudoClojure.pperceptron
 ; (:use [LudoClojure.spindle])
 ; (:use [LudoClojure.spinopencl])
  (:use [LudoClojure.opencl-utils])
  (:use [LudoClojure.utils])
  (:use [calx])
 ;(:require clojure.math.numeric-tower)
  )
;;(use 'calx)
;;TODO use as to fuly quilify functions, make it easy to trace back to implementations.

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
;;TODO for best practice, deplay execution of this untill -main or test time.
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
  
  vecProductResult[gid] =  total;
//if( isnan(total) == 1 || fabs(total) < 0.00000001 ) {
//      vecProductResult[gid] = 0.0;
//     }
//    else {
//      vecProductResult[gid] =  total;
//    }
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

if (total < -rho) {  
    pp_answer_buf[gid] = -1.0;
    }
else if (total > rho) {  
    pp_answer_buf[gid] = +1.0;
    }
else {
   // pp_answer_buf[gid] = convert_float4_rtp(total / convert_float(rho)) ;    //TODO use a safer function?
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
int pp_we_are_talking_to =  convert_int_rtz(gid / pp_size);   //TODO use a safer function? Is this rounding up at 0.5?!!


float pp_answer          = pp_answer_buf[pp_we_are_talking_to];

float correct_answer_val = correct_answer_buf[pp_we_are_talking_to];
float upper_correct      = correct_answer_val + epsilon;
float lower_correct      = correct_answer_val - epsilon;
float vProductResult     = vecProductResult[gid];

//WIP computing the norm of the alphas for this perceptron
float alphapart            = 0.0;
float alpha_norm_squared   = 0.0;
for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha_norm_squared +=  (alphapart * alphapart);
       }

float alpha_norm_squared_adjustment_prep  = (alpha_norm_squared - 1.0) * eta;      //
float alpha_norm_squared_adjustment = 0.0;
if( isnan(alpha_norm_squared_adjustment_prep) == 1  || isinf(alpha_norm_squared_adjustment_prep) == 1 ||  fabs(alpha_norm_squared_adjustment_prep) < 1.0E-15 ||  fabs(alpha_norm_squared_adjustment_prep) > 1000000.0 ){
      alpha_norm_squared_adjustment = 0.0;
      }
      else
     {
     alpha_norm_squared_adjustment = alpha_norm_squared_adjustment_prep;
     }

float alpha_prewrite = 0.0;
if ((pp_answer > upper_correct) && (vProductResult >= 0.0)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha_prewrite =  alphapart - (alpha_norm_squared_adjustment * alphapart) - (input_data[k] * eta);
                 if( isnan(alpha_prewrite) == 1 || isinf(alpha_prewrite) == 1 ||  fabs(alpha_prewrite) < 1.0E-15 ||  fabs(alpha_prewrite) > 1000000.0  ) {
                      //do nothing if there is a problem    alpha[perceptronid + k] = 0.0;
                      }
                 else {
                       alpha[perceptronid + k] =  alpha_prewrite;
                       }
       }
}
else if ((pp_answer < lower_correct) && (vProductResult < 0.0)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha_prewrite = alphapart - (alpha_norm_squared_adjustment * alphapart) + (input_data[k] * eta);
                 if( isnan(alpha_prewrite) == 1 || isinf(alpha_prewrite) == 1 ||  fabs(alpha_prewrite) < 1.0E-15 ||  fabs(alpha_prewrite) > 1000000.0 ) {
                      //do nothing if there is a problem    alpha[perceptronid + k] = 0.0;
                      }
                 else {
                       alpha[perceptronid + k] =  alpha_prewrite;
                       }
       }
}
//Here we Stabilizing the outputs around zero
else if ((pp_answer <= upper_correct) && (0.0 <= vProductResult) && (vProductResult < gama)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];
         alpha_prewrite = alphapart - (alpha_norm_squared_adjustment * alphapart) + (input_data[k] * eta * mu) ;
                 if( isnan(alpha_prewrite) == 1 || isinf(alpha_prewrite) == 1 ||  fabs(alpha_prewrite) < 1.0E-15 ||  fabs(alpha_prewrite) > 1000000.0 ) {
                      //do nothing if there is a problem   alpha[perceptronid + k] = 0.0;
                      }
                 else {
                       alpha[perceptronid + k] =  alpha_prewrite;
                       }
       }
}
else if ((pp_answer >= lower_correct) && (vProductResult < 0.0)    && (-gama < vProductResult)){
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];

         alpha_prewrite = alphapart - (alpha_norm_squared_adjustment * alphapart) - (input_data[k] * eta * mu) ;
                 if( isnan(alpha_prewrite) == 1 || isinf(alpha_prewrite) == 1 ||  fabs(alpha_prewrite) < 1.0E-15 ||  fabs(alpha_prewrite) > 1000000.0 ) {
                      //do nothing if there is a problem   alpha[perceptronid + k] = 0.0;
                      }
                 else {
                       alpha[perceptronid + k] =  alpha_prewrite;
                       }
       }
}
else {
       for(int k=0; k<input_size; k++) {
         alphapart = alpha[perceptronid + k];

         alpha_prewrite = alphapart - (alpha_norm_squared_adjustment * alphapart) ;
                 if( isnan(alpha_prewrite) == 1 || isinf(alpha_prewrite) == 1 ||  fabs(alpha_prewrite) < 1.0E-15 ||  fabs(alpha_prewrite) > 1000000.0 ) {
                      //do nothing if there is a problem   alpha[perceptronid + k] = 0.0;
                      }
                 else {
                       alpha[perceptronid + k] =  alpha_prewrite;
                       }
       }
}
}
"
}})

;;Compile openCL kernels into the opencl environment
;;TODO should make this a function, so this happens only at main 'run time'
;;(opencl_env_compileprogs opencl_env (get_openCL_from_kernels opencl_kernels))
(opencl_compile_default)

;;(make_random_float_array 100 -0.5 1)


(defprotocol PperceptronProtocol
  "Protocol for asking and teaching a paralel perceptron."
  (pp_vecproduct [pp] "Part one stage of computig a pp answer. Takes input data that and alphas and does a vector product over them")
  (pp_reduceToPP [pp] "Part two takes the vector products result and adds it up for each pp, giving an integer which is the pp answer, this is scaled down to between -1.0 and 1.0")
  (pp_updateAlphas [pp] "The 3rd stage does the learning by updating the alphas vector by considering the correct answer provided and the answer given by the current pp")
  (pp_wait_for_event [pp openCLevent] "makes the pp queue wait for the given event")
  (pp_enqueue_marker [pp] "enque a marker on the pp openCL queue")
  (pp_write_input [pp data_vec] "write a float vector, or an openCL buffer, to the pp input data vector")
  (pp_write_correct_answer [pp data_vec] "write a float vector, or an openCL buffer, to the pp correct answer data vector")
  (pp_readout [pp buffkey] "read out one of the pp buffers")
  (pp_train  [pp input output] [pp input output options] "train a pp on the input, correct output pair, optionaly do so with different then default meta parameters")
  (pp_train_and_answer [pp input output] [pp input output options] "train a pp on the input, correct output pair, optionaly do so with different then default meta parameters, and also return pp answer")
  (pp_answer [pp input] "get pp answer to a specific input. No training is done")
  )

(defrecord PperceptronRecord [alpha_buf 
                        input_data_buf
                        vecProductResult_buf
                        correct_answer_buf
                        pp_answer_buf
                        pp_opencl_env
                        pp_queue
                        input_size
                        outputs_size
                        pp_size
                        rho
                        eta
                        gama
                        epsilon
                        mu
                        ]
  PperceptronProtocol
(pp_vecproduct [pp]
  "Part one stage of compoutig a pp answer. Takes input data that and alphas and does a vector product over them"
  (lg_enqueue-kernel ((:pp_queue pp) @(:pp_opencl_env pp)) (:progs @(:pp_opencl_env pp))
      :vecProduct  (* (:pp_size pp) (:outputs_size pp))    ;;global size, here number of perceptrons in the one pp
                          (:input_size pp)  ;;inluding the bias term
                          (:input_data_buf pp)
                          (:alpha_buf pp)    ;;should be 
                          (:vecProductResult_buf pp)))

(pp_reduceToPP [pp]
  "Part two takes the vector products result and adds it up for each pp, giving an integer which is the pp answer, this is scaled 
down to between -1.0 and 1.0"
  (lg_enqueue-kernel ((:pp_queue pp) @(:pp_opencl_env pp)) (:progs @(:pp_opencl_env pp))
                          :reduceToPP
                          (:outputs_size pp) ;;global size, here total number of perceptrons, here just one
                          (:pp_size pp)
                          (:rho  pp)
                          (:vecProductResult_buf pp)
                          (:pp_answer_buf pp)))

(pp_updateAlphas [pp]
 "The 3rd stage does the learning by updating the alphas vector by considering the correct answer provided and the answer given by the current pp"
  (lg_enqueue-kernel ((:pp_queue pp) @(:pp_opencl_env pp)) (:progs @(:pp_opencl_env pp))
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

(pp_wait_for_event [pp openCLevent]
;;TODO really need to learn defrecord, defprotocol etc so that each openCL wrapper object can reuse the same function
"makes the pp queue wait for the given event"
   (lg_enqueue-wait-for (@( :pp_opencl_env pp) (:pp_queue pp)) openCLevent ))

(pp_enqueue_marker [pp]
    (lg_enqueue-marker (@(:pp_opencl_env pp)(:pp_queue pp))))

;;TODO refactor both write fuctions
;; pp clojure reals vector familiy of functions
(pp_write_input [pp data_vec ]
  ;;TODO add check for size to be as expects
  (cond (is_buffer? data_vec)
         (openCL_copy_buf_to_buf! ((:pp_queue pp) @(:pp_opencl_env pp)) (:progs @(:pp_opencl_env pp)) :copyFloatXtoY
                data_vec  ;copy data from here
                (:input_data_buf pp)) ;to here
        (vector? data_vec)
           (let [data_float_vec (map float data_vec)]
              (lg_enqueue-overwrite (:input_data_buf pp) [(- (:input_size pp)) 0] (to-buffer data_float_vec :float32-le) ((:pp_queue pp) @(:pp_opencl_env pp)))
              (pp_enqueue_marker pp))
))

(pp_write_correct_answer [pp data_vec ]
  ;;TODO add check for size to be as pp expects
  (cond (is_buffer? data_vec)
         (openCL_copy_buf_to_buf! ((:pp_queue pp) @(:pp_opencl_env pp)) (:progs @(:pp_opencl_env pp)) :copyFloatXtoY
                data_vec  ;copy data from here
                (:correct_answer_buf pp)) ;to here
        (vector? data_vec)
          (let [data_float_vec (map float data_vec)]
            (lg_enqueue-overwrite (:correct_answer_buf pp) [(- (:outputs_size pp)) 0] (to-buffer data_float_vec :float32-le) ((:pp_queue pp) @(:pp_opencl_env pp)))
            ;REMOVED(lg_finish ((pp :pp_queue) @(:pp_opencl_env pp)))
            (pp_enqueue_marker pp))
))

(pp_readout [pp buffkey]
  ;;TODO make :pp_answer_buf the default read out
  @(lg_enqueue-read (buffkey pp) ((:pp_queue pp) @(:pp_opencl_env pp))))
 ; @(lg_enqueue-read (:input_data_buf pp) ((:pp_queue pp) @(:pp_opencl_env pp))))


(pp_train [pp input output]
"Just give an input output pair, which can be a float array or buffer,
   be sure to include a -1.0 entry alawys and consistently in one of the possition, ie: the normalising entry  "
  ;;TODO quite sure all these finishes are not necessary
  (do
  (pp_write_input pp input)
  (pp_write_correct_answer pp output)
  (pp_vecproduct pp)
  (pp_reduceToPP pp)
  (pp_updateAlphas pp)))
(pp_train [pp input output options]
"Just give an input output pair, which can be a float array or buffer,
   be sure to include a -1.0 entry alawys and consistently in one of the possition, ie: the normalising entry  "
  ;;TODO quite sure all these finishes are not necessary
 (let [pp (conj pp options)]
  (do
  (pp_write_input pp input)
  (pp_write_correct_answer pp output)
  (pp_vecproduct pp)
  (pp_reduceToPP  pp)
  (pp_updateAlphas  (conj pp options))
)))

(pp_train_and_answer [pp input output]
  ;;TODO quite sure all these finishes are not necessary
  (do
  (pp_write_input pp input)
  (pp_write_correct_answer pp output)
  (pp_vecproduct pp)
  (pp_reduceToPP  pp)
  (pp_updateAlphas pp )
  ;;(pp_updateAlphas  pp)
  (pp_readout pp :pp_answer_buf)
  ))
(pp_train_and_answer [pp input output options]
  ;;TODO quite sure all these finishes are not necessary
 (let [pp (conj pp options)]
  (do
  (pp_write_input pp input)
  (pp_write_correct_answer pp output)
  (pp_vecproduct pp)
  (pp_reduceToPP  pp)
  (pp_updateAlphas  (conj pp options))
  ;;(pp_updateAlphas  pp)
  (pp_readout pp :pp_answer_buf)
  )))

(pp_answer [pp input]
 "TODO : this is not thread safe, 2 threads doing this will bash the same GPU data..."
 (do
  ;;(lg_finish ((pp :pp_queue) @(:pp_opencl_env pp)))  ;ensure any queue work finishes
  (pp_write_input pp input)
  (pp_vecproduct pp)
  (pp_reduceToPP  pp)
  (pp_readout pp :pp_answer_buf)
  ))

)



(defn make_pp 
    "Returns a Pperceptron initialised as per specification"
  ;[opencl_env  input_size  outputs_size  pp_size rho]
  [options]
  "TODO use destructuring to pass in a map that configures the pp, provide defaults"
  (let [{:keys [pp_opencl_env input_size outputs_size pp_size rho eta gama epsilon mu rho pp_queue] 
                 :or 
                {pp_opencl_env opencl_env 
                 input_size 3
                 outputs_size 1
                 pp_size 3
                 rho 1
                 eta (float 0.001)
                 gama (float 0.4)
                 epsilon (float 0.0499)
                 mu (float 0.9 )
                 pp_queue :queue  ;The default queue in the opencl_env
                 }
                } options
        pp_queue_a (if (@pp_opencl_env pp_queue)  ;; if queue does not exist in the opencl_env, add it
                       pp_queue
                      (do (opencl_env_addQueue pp_opencl_env pp_queue) pp_queue))
        size_of_alpha_needed (* input_size pp_size outputs_size)
        input_data_buf       (doall (lg_create-buffer (:context @pp_opencl_env) input_size :float32-le))
        alpha_buf            (doall (lg_wrap (:context @pp_opencl_env) (make_random_float_array size_of_alpha_needed -0.5 1) :float32-le))
        vecProductResult_buf (doall (lg_create-buffer (:context @pp_opencl_env) (* pp_size outputs_size)  :float32-le))
        correct_answer_buf   (doall (lg_create-buffer (:context @pp_opencl_env) outputs_size :float32-le))
        pp_answer_buf        (doall (lg_create-buffer (:context @pp_opencl_env) outputs_size :float32-le))
        ]

 (comment "initial map based implementation before moving to defrecord"
          { :alpha_buf            alpha_buf
            :input_data_buf       input_data_buf
            :vecProductResult_buf vecProductResult_buf
            :correct_answer_buf   correct_answer_buf
            :pp_answer_buf        pp_answer_buf
            :pp_opencl_env        pp_opencl_env
            :pp_queue             pp_queue_a
            :input_size           input_size
            :outputs_size         outputs_size
            :pp_size              pp_size
   
            :rho                  rho           ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
            :eta                  eta           ;;  learning_rate
            :gama                 gama          ;;  margin around zero
            :epsilon              epsilon       ;;  level of error that is allowed.
            :mu                   mu            ;;  learning modifier around zero
   })
  
 (PperceptronRecord. alpha_buf 
               input_data_buf 
               vecProductResult_buf
               correct_answer_buf
               pp_answer_buf
               pp_opencl_env
               pp_queue_a
               input_size
               outputs_size
               pp_size
               rho                   ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
               eta                   ;;  learning_rate
               gama                  ;;  margin around zero
               epsilon               ;;  level of error that is allowed.
               mu                    ;;  learning modifier around zero
  )))



(comment
(def a_pp (make_pp {:input_size 5
                   :outputs_size 3
                   :pp_size 3
                   :rho 1                    ;;  accuracy to which pp should learn, 1 means give back a binary 1,-1 output, 2means 1,0,-1, assuming pp is of an odd size etc.
                   :eta (float 0.01)        ;;  learning_rate
                   :gama (float 0.4)         ;;  margin around zero              ;0.4
                   :epsilon (float 0.049)    ;;  level of error that is allowed.
                   :mu (float 0.9 )}))

a_pp
@(:pp_opencl_env ^Pperceptron a_pp)
(:pp_queue a_pp)
(:pp_answer_buf a_pp)
(time (pp_readout a_pp :input_data_buf))
)



;;;Example finish maybe makes sense if there are not queued buffer writes lg_enqueue-overwrite or lg_enqueue-read
;(lg_finish ((pp :pp_queue) @(:pp_opencl_env pp)))


;;-------Scratch starts here--------------------------------------------------------------------------






















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
  ;;Assume a single openCL env? Needs to know sizes of pp

  ;opencl_env
  ;testing kernel compilation 
)


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