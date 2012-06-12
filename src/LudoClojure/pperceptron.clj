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
pp_answer_buf[gid] = total / pp_size;  //Different squashing functions could be implemente here, eg: binary 1,-1   devide by pp_size is actually incorrect.
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
    __global float *alpha,
    __global int *debug_buff_int
    )
{

float total = 0.0;
int gid = get_global_id(0);



// which perceptron are we talking to
int perceptronid = gid * input_size;

//which pp are we taking pp_answer_buf and correct_answer_buf for?
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
else if ((pp_answer >= lower_correct) && (-gama < vProductResult) && (vProductResult < 0)){
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


(quote
  TODO
  API functions to wrap over pp

  
  (defn make_pp [opencl_env input_size outputs_size pp_size] {...buffers (atom of configs)})
        ;; sets up all buffers, default config)
  (defn update_pp_config [pp {of options }] (swap! pp))   ;; to update learning rates etc...)  ;; meta learning functionality here, eg: to optimise learning rate etc?
  (defn train_pp!   (pp in_vec correct_answer_vec))   ;my need to expose openCL queue ; my wish to pass in buffers for speed, ie: have fast version of these where queue and nonCL buffers are passed in,.. or even target openCL buffers (comes in under openCL interoprility) 
  (defn readout_pp! (pp in_vec answer_vec))
  (defn train_examples [pp, traincycles, vec of in_vecs, vec of correct_answer_vecs] trains pp for n staps, emit assesment of accuracy)
  
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
(opencl_env_compileprogs opencl_env (get_openCL_from_kernels opencl_kernels))



;see which kernel programs
(:progs @opencl_env)


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
                          vecProductResult
                          pp_answer_buf))
(defn reduceToPPq [q]
(lg_enqueue-kernel q (:progs @opencl_env) 
                          :reduceToPP
                          number_of_pps ;;global size, here total number of perceptrons, here just one
                          pp_size
                          vecProductResult
                          pp_answer_buf))

@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))
(reduceToPP)
@(lg_enqueue-read pp_answer_buf (:queue @opencl_env))



(def eta           (float 0.01)) ;;  learning_rate
(def gama          (float 0.05)) ;;  margin around zero
(def epsilon       (float 0.001)) ;;  level of error that is allowed.
(def mu            (float 0.5 )) ;;  learning modifier around zero


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

(lg_finish (:queue @opencl_env))

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






