(ns LudoClojure.pperceptron
  (:use [LudoClojure.spindle])
  (:use [LudoClojure.spinopencl])
  )
;(use 'calx)

(+ 1 2)

;TODO define what we want
;Implement it



;; Naming convention
;;   pps          is an array of perceptron (not being impelented in the early stage, but this is the target)
;;   pp           is one paralel perceptron
;;   perceptron   is one of the perceptrons within a pp
;;   alpha        holds over all pps, each pp and each perceptron, the weight poiting to input data arrray



(add_kernel_specs opencl_kernels
{
:vecProduct {
     :desc "computes a vector product of an *in sized array by *in time *out  
vector, resultingin *out sized array, notes on approach here:
http://www.bealto.com/gpu-gemv_v1.html"
     :postion 10
     :body "
__kernel void vecProduct(
    const int input_size,
    __global float *in,
    __global float *alpha,
    __global float *out
    )
{

float total = 0.0;
int gid = get_global_id(0);

  for(int k=0; k<input_size; k++) {
       total += alpha[gid * input_size + k] * in[k];
  }
out[gid] = total;
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
        if( binaryPPout > 0) {
            total += 1.0; 
           }
        else {
            total += -1.0;
             }
  }
pp_answer_buf[gid] = total / pp_size;  //Different squashing functions could be implemente here, eg: binary 1,-1 
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
       const int   input_size,
       const float epsilon,
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

/////  int epislons_per_pp = input_size * number_of_pps;

//which pp are we taking pp_answer_buf and correct_answer_buf for?
int pp_we_are_talking_to = gid / pp_size;


float pp_answer      =      pp_answer_buf[pp_we_are_talking_to];
float upper_correct  = correct_answer_buf[pp_we_are_talking_to] + epsilon;
float lower_correct  = correct_answer_buf[pp_we_are_talking_to] - epsilon;
float vProductResult = vecProductResult[gid];


if ((pp_answer > upper_correct) && (vProductResult >= 0.0)){
       for(int k=0; k<input_size; k++) {
       alpha[gid * input_size + k] =  alpha[gid * input_size + k]  - input_data[k];
     }
}

if ((pp_answer < lower_correct) && (vProductResult < 0.0)){
       for(int k=0; k<input_size; k++) {
       alpha[gid * input_size + k] =  alpha[gid * input_size + k]  + input_data[k];
     }
}

//compute the per perceptron values, ie: which direction is the perceptron wrong.
  for(int k=0; k<input_size; k++) {
     ///write to the alpha entry conditionally    = WIP;
   // alpha[gid * input_size + k] =  alpha[gid * input_size + k] 
  }
}
"
}})


(def pp_spindle (make_spindle 1000 1))
;Load in openCL kernels
(spindle_add_openCLsource! pp_spindle (get_openCL_from_kernels opencl_kernels))
;Start the openCL spindle used in all subsequent tests
(start_spindle! pp_spindle)



;;Note, -1.0 at the end always needs to be there, this is the bias term.
(def input_data_buf_atom  (atom (make_buf pp_spindle [1.0 0.0 -1.0] :float32)))


(defn make_random_float_array [size booster seed]
   (doall
   (let [r (java.util.Random. seed)]
     (loop [i 0 outlist []]
       (if (= i size)
         outlist
         (recur (inc i) (conj outlist (+ (* (float (.nextInt r 100)) 0.01) booster) )))))))

(make_random_float_array 5 -0.5 1)
(make_random_float_array 6 -0.5 1)

(def pp_size 5)
(def input_size (buf_elements @input_data_buf_atom))
(def size_of_alpha_needed (* input_size pp_size))

(def alpha   (make_buf pp_spindle (make_random_float_array size_of_alpha_needed -0.5 1) :float32))


(read_buf pp_spindle alpha)
(read_buf pp_spindle @input_data_buf_atom)



;;example  of swapping in a new buffer with new input data
(swap! input_data_buf_atom (fn [_] (make_buf pp_spindle [1.0 1.0 -1.0] :float32)))
(def first_pp_target_ouput
[(+ 0.35 0.38 0.03) 
 (+ -0.37 0.04 0.46)
(+ -0.16 -0.44 -0.28)
(+ -0.02 0.19 -0.23)
(+ -0.33 0.13 -0.12)])

(def vecProductResult (make_empty_buf pp_spindle pp_size :float32))
(read_buf pp_spindle vecProductResult)

;This is for one pp only...
(weave_kernel! pp_spindle :vecProduct 
                          pp_size    ;;global size, here number of perceptrons in the one pp
                          input_size  ;;inluding the bias term
                          @input_data_buf_atom 
                          alpha    ;;should be 
                          vecProductResult)

(read_buf pp_spindle vecProductResult)

(def correct_answer_buf (atom (make_buf pp_spindle [1.0] :float32)))
(def pp_answer_buf (make_empty_buf pp_spindle 1 :float32))
(def number_of_pps (buf_elements @correct_answer_buf))

(read_buf pp_spindle @correct_answer_buf)  ;;The answer for the one buf (given the input data at this time
(read_buf pp_spindle pp_answer_buf)

(weave_kernel! pp_spindle :reduceToPP
                          number_of_pps ;;global size, here total number of perceptrons, here just one
                          pp_size
                          vecProductResult
                          pp_answer_buf)

(reduce + (read_buf pp_spindle vecProductResult))
(read_buf pp_spindle pp_answer_buf)

;;need to intoduce epsilon (funny e), the allowable error range
;;vecProductResult , pp_answer_buf and  correct_answer_buf not need to be brodcasted out to all the alpha entries, do determine what should be updated

(def epsilon (float 0.01))    ;; The level of error that is allowed.
;;Error direction
(class epsilon)

(def debug_buff_int (make_empty_buf pp_spindle 30 :int32))
(read_buf pp_spindle debug_buff_int)

(weave_kernel! pp_spindle :updateAlphas
                          (* pp_size number_of_pps)        ;global size, here number of perceptrons in the one pp * number_of_pps
                          number_of_pps     ; we will need to bring in global data relavant to the pp to each perceptron within, a kind of join, 
                          pp_size
                          input_size          ;will be looping using this parameter to update all alphas
                          epsilon
                          @input_data_buf_atom
                          vecProductResult
                          pp_answer_buf
                          @correct_answer_buf
                          alpha
                          debug_buff_int
                          ;;Strategy is compute all global values first, then loop over the alpha buffer to apply updates if needed
                          )


(read_buf pp_spindle debug_buff_int)
(read_buf pp_spindle vecProductResult)
(read_buf pp_spindle alpha)
(read_buf pp_spindle @input_data_buf_atom)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;






























(quote
  testing loading buffers quickly
(time (def foo (make_random_float_array 10000 -0.5 1)))
(time (loop [i 0 a (atom :a)]
    (if (= i 60)
        input_data_buf_atom
     (recur (inc i) (swap! input_data_buf_atom (fn [_] (make_buf pp_spindle (make_random_float_array 10000 i 1) :float32))))
      )))
)


(count (read_buf pp_spindle @input_data_buf_atom))
(first (read_buf pp_spindle @input_data_buf_atom))

(def foo_out (read_buf pp_spindle @input_data_buf_atom))
(class foo_out)
(count foo_out)





;(stop_spindle! pp_spindle)





(quote

  dfsfsd ert e
  
  )






(quote

(def pp_openCL (str pp_openCL pp_openCL2 openCL_copy_float_x_to_y))
;;Note is the set of kernels to be openCL compiles keeps growing we may hit openCls limit 
;;To build up the set of kernels across projects eg: pp and lsm, create a kernels entity which will concat the strigs just before passing to spindle
;;so something like:


(def opencl_kernels (atom
 ;Array of kernel names to their specs   ;;;this will have to be in core??? the clj files just ram into here?..then they don't compose....
    {}))

;;simple  just compose each clj's  kernels just before passing into spindle
;(conj @kernels @kernels @kernels)

(defn add_kernel_specs [kernels_map kernel_specsmap]
   (swap! kernels_map (fn [kernels_map] (conj kernels_map kernel_specsmap))))

;;To add a kernel then do:
(add_kernel_specs opencl_kernels
{
"newkernel" {:desc "testaddedkernel"
             :postion 3
             :body "
__kernel void testaddedkernel(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid] - 1.0;
}
"}})

(add_kernel_specs opencl_kernels
{
"copyFloatXtoY" {
     :desc "copies one float array to another"
     :postion 1
     :body "
__kernel void copyFloatXtoY(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid];
}
  "
}
"foo1" {
     :desc "takes float x and puts x+1 into y"
     :postion 2
     :body "
__kernel void foo1(
    __global float *x,
    __global float *y
    )
{
    int gid = get_global_id(0);
    y[gid] = x[gid] + 1.0;
}
"
}
})

(defn get_openCL_from_kernels [a_kernels_map]
  (reduce str 
      (map :body 
         (sort-by :postion 
            (vals @a_kernels_map)))))

(get_openCL_from_kernels opencl_kernels)

;;TODO question, will this work well across clj files?

(quote
(keys @kernels)
(keys @kernels)
(keys @kernels)
(:body (@kernels "newkernel"))
(map :body (vals @kernels))
(sort < [1 1 2])
(reduce str (map :body (sort-by :postion (vals @kernels))))
;;Does the order of submited kernels matter,it does, what then with support functions that get used within
(map :body (vals @kernels))
(concat (map :body (vals @kernels)))
(str (dorun (map :body (vals @kernels))))
(str (doall (map (fn [x] (:body x)) (@kernels "newkernel")))))


;;<<<< Clean to here
;;defs
(def default_spindle (make_spindle 2000 1 :default_spindle))


(defn make_float_buf
 ([float_array]
   (make_float_buf default_spindle float_array))
 ([spindle float_array]
   (weave! spindle #(wrap float_array :float32))))


;;empty bufs (weave! default_spindle #(create-buffer (* 64 64 64) :float32 ))

(quote ;This will catastrophically fail a spindle
(defn forceFailSpindle [spindle number_of_elements]
    (weave! spindle #( (create-buffer number_of_elements :float32))))

(forceFailSpindle default_spindle 10)
)

(defn make_empty_float_buf [spindle number_of_elements]
    (weave! spindle (fn [] (create-buffer number_of_elements :float32))))

(defn make_int_buf 
 ([int_array]
   (make_int_buf default_spindle int_array))
 ([spindle int_array]
   (weave! spindle #(wrap int_array :int32))))


(defn read_buf
 ([buf]
    (read_buf default_spindle buf))
 ([spindle buf]
    (weave! spindle (fn [] @(enqueue-read buf)))))


(defn make_random_float_array [size]
  "returns a deterministic random float array vector"
   (doall
   (let [r (java.util.Random. 12345)]
     (loop [i 0 outlist ()]
       (if (= i size)
         outlist
         (recur (inc i) (conj outlist (float(* (.nextInt r 1000) 0.001)))))))))


(defn buf_elements [buf]
     (:elements buf))

;;TODO make empty buf

(comment 
 ;; first blood with macros
(weave_kernel :foo 1 2 3 4) 
(reduce + '(1 2 3 4))
(defmacro dbg[x] `(let [x# ~x] (println '~x "=" x#) x#))
(defn pythag [ x y ] (* (* x x) (* y y)))
(defn pythag [ x y ]  (dbg (* (dbg (* x x)) (dbg (* y y)))))
(pythag 4 5)
(defmacro weave_kernel [spindle & args]
        `(+ ~@args))
(macroexpand '(weave_kernel :foo 12 234 32 23 23  2 23))
(macroexpand-1 '(weave_kernel :foo 12 234 32 23 23  2 23)))


(defmacro weave_kernel! [spindle kernel_keyword globalsize & bufs]
  "Side effects only, weaves away the buffer on the given spindle"
       `(weave_away! ~spindle #(enqueue-kernel ~kernel_keyword ~globalsize ~@bufs)))


(defn copy_float_buf_to_buf2 [buf1 buf2]
   (weave_kernel! default_spindle :copyFloatXtoY (buf_elements buf1) buf1 buf2))

(defn copy_float_buf_to_buf [buf1 buf2]
   (weave_away! default_spindle #(enqueue-kernel :copyFloatXtoY (buf_elements buf1) buf1 buf2)))
;;TODO how to make the spindle go away from the set of parameters without shooting oneself in the foot longterm?
;;TODO Keep passin the spindle in everywhere



(time (is_spindle? default_spindle))

(defn readin_to_buf [buf float_array]
  ;TODO replace this with float and int versions
    (copy_float_buf_to_buf (make_float_buf float_array) buf))

(defn readin_to_float_buf [buf float_array]
    (copy_float_buf_to_buf (make_float_buf float_array) buf))


;---PP code

(def training_set_XOR
  ; "array of input output arrays, answers must be between -1 and 1"
  ;[[inputs] [correctvalues]]   [[inputs] [correctvalues] [answers]]
   [
    {:in [1 0] :out [1]}
    {:in [1 1] :out [-1]}
    {:in [0 1] :out [1]}
    {:in [0 0] :out [-1]}
    ])

(defn make_problem_options [problem]
    {:examples (count problem)
     :input_size  (count (:in (first problem)))
     :output_size  (count (:out (first problem)))
     })

(make_problem_options training_set_XOR)

(def test_internal_options  {:hidensize 5
                             :learning_rate 0.05
                             :zero_margin 0.05
                             :zero_margin_learning_rate 1.0
                             })



(defn pp_make [spindle problem_options pp_internal_options]
  ;;returns a pp represented as map to bufs on givien spindle and pp method functions

   {:spindle spindle    ;;The spindle this pp will spin on.
    :input_to_hiden_weights (make_float_buf spindle (make_random_float_array 
                                                     (* (:input_size problem_options)
                                                        (:output_size problem_options)
                                                        (+ 1 (:hidensize pp_internal_options)))))
;;TODO create all other bugs relavant to a pp here
    })


;;TODO create support functions that take a pp, lookinto it for relavant bufs, and mutate state on the spindle ref held in the pp
;;TODO put purely openCL funs to a seperate module, have them all take spindle as first parameter

;;Test it in tests file...





;;empty bufs (read_buf (weave! default_spindle #(create-buffer (* 4) :float32 )))

;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff
;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff
;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff;;-----DoingStuff
;--Setup
(spindle_add_openCLsource! default_spindle (str pp_openCL))
(start_spindle! default_spindle)
(spin_dump! default_spindle)
;;(stop_spindle! default_spindle)

(def test_pp (pp_make default_spindle (make_problem_options training_set_XOR) test_internal_options))
(read_buf (:input_to_hiden_weights test_pp))


(def test_empty_buf2 (make_empty_float_buf default_spindle 12))
(read_buf test_empty_buf2)
(readin_to_buf test_empty_buf2 [2.2 12.99 3.3])
(read_buf test_empty_buf2)


(def test_empty_buf5 (make_empty_float_buf default_spindle 12))
(read_buf test_empty_buf5)
(readin_to_buf test_empty_buf5 [1.0 2.0 3.0])
(read_buf test_empty_buf5)
(def test_empty_buf6 (make_empty_float_buf default_spindle 12))
(read_buf test_empty_buf6)
(def test_empty_buf7 (make_empty_float_buf default_spindle 13))
(read_buf test_empty_buf7)
(def test_empty_buf8 (make_empty_float_buf default_spindle 14))
(read_buf test_empty_buf8)
(def test_empty_buf9 (make_empty_float_buf default_spindle 12))
(read_buf test_empty_buf9)
(readin_to_buf test_empty_buf9 [1.0 2.0 3.999])
(def test_empty_buf10 (make_empty_float_buf default_spindle 3))
(read_buf test_empty_buf10)
(readin_to_buf test_empty_buf10 [1.0 2.0 4.999 4.6])
(read_buf test_empty_buf10)

;defs
(def float_array (map float [1.1 1.2 1.3 1.123456789]))
(def int_array [1 2 3 4 5])
(def test_float_buf (make_float_buf float_array))
(time (def test_int_buf (make_int_buf int_array)))
(time (def random_float_array (make_random_float_array 100)))
(time (def test_random_float_buf (make_float_buf random_float_array)))




;reads
(read_buf test_int_buf)

(read_buf test_float_buf)
(def test_float_buf2 (make_float_buf [10.1 20.1 30.1])) 
(read_buf test_float_buf)
(read_buf test_float_buf2)

(read_buf test_random_float_buf)
;(read_buf random_float_array) ;;  !! trying to read an array with a buffer function!! break spindle
(time (def comback_test_random_float_array (read_buf test_random_float_buf)))

(time (copy_float_buf_to_buf test_float_buf test_random_float_buf))
;(time (copy_float_buf_to_buf test_random_float_buf test_float_buf ))
(comment
(time (loop [k 7000]
    (if 
      (= k 0)
          (time(read_buf test_random_float_buf))
          ;:done
          (do (copy_float_buf_to_buf test_float_buf test_random_float_buf)
             (recur (dec k))))))

(time (is_spindle? default_spindle))

(time (loop [k 100]
    (if 
      (= k 0)
          ;(time(read_buf test_random_float_buf))
          :done
          (do (is_spindle? default_spindle)
             (recur (dec k))))))

)



(comment
;(weave! spindle #(enqueue-kernel kernel 3 args1))
(spin_kernel default_spindle :foopp 1 2)
(weave! default_spindle #(enqueue-kernel :foopp 3 test_float_buf test_float_buf))
(read_buf test_float_buf)
)

;tests
(= float_array (read_buf test_float_buf))
(time (= int_array (read_buf test_int_buf)))
(time (= comback_test_random_float_array (read_buf test_random_float_buf)))

(readin_to_buf test_float_buf [2.2 12.2])




;;-----DoingStuff DONE












(comment mocs

         ;Old stuff that will end up in scratch



(time (weave! pp_spindle #(let [inarray [0.01 10.10 2.00001]
               foo (wrap inarray :float32)]
           @(enqueue-read foo ))))
(weave! pp_spindle #(wrap [0.01 14.10 2.00001] :float32))
(weave! pp_spindle #(def foo (wrap [0.01 14.10 2.00001] :float32)))
(weave! pp_spindle #(def baz (wrap [34.01 14.10 2.00001] :float32)))


(defn make_buf [spindle arrayvals]
   (weave! spindle #(wrap arrayvals :float32)))





(def test_float_buf (make_float_buf pp_spindle [0.1 0.2 0.3]))
(def test_int_buf (make_int_buf pp_spindle [0.1 0.2 0.3]))




;;TODO make buf_int
;;TODO make buf_float

(def mybuf (make_buf pp_spindle [34.01 14.10 2.00001 98.2]))

(defn buf_elements [buf]
     (:elements buf))

(buf_elements mybuf)



                

(time (weave! pp_spindle #(+ 1 2)))
(time (+ 1 2))
(weave! pp_spindle (fn [] @(enqueue-read foo)))
(weave! pp_spindle (fn [] @(enqueue-read baz)))

(weave! pp_spindle #(enqueue-kernel :foopp 3 foo baz))
(weave! pp_spindle (fn [] @(enqueue-read foo)))
(weave! pp_spindle (fn [] @(enqueue-read baz)))


(defn openCL_increment [spindle floatarray]
  (let [calxbuffer (weave! spindle #(wrap floatarray :float32))]
     (weave! spindle #(enqueue-kernel :foopp 3 calxbuffer calxbuffer))
     (weave! spindle (fn [] @(enqueue-read calxbuffer)
      calxbuffer))))

(time (openCL_increment pp_spindle [12.01 11.10 2.00001]))

(defn openCL_increment_persist [spindle buf1 buf2]
      (weave! spindle #(enqueue-kernel :foopp 3 buf1 buf2))
       buf2)

(defn buf_read [spindle buf]
    (weave! spindle (fn [] @(enqueue-read buf))))

(buf_read pp_spindle (openCL_increment_persist pp_spindle foo foo))

(buf_read pp_spindle (openCL_increment_persist pp_spindle mybuf mybuf))



(time (loop [k 300]
    (if 
      (= k 0)
          (buf_read pp_spindle (openCL_increment_persist pp_spindle mybuf mybuf))
         (do (buf_read pp_spindle (openCL_increment_persist pp_spindle mybuf mybuf))
             (recur (dec k))))))



(defn make_pp [spindle size options]
  

(stop_spindle! pp_spindle)

)
;;TODO   Need generic openCL-clojure functionality to move data about

;Deal with array types.... nail ints and floats.

;center on buffers or the data they represent??? or the version of the double buffer thats important now?





(time (with-cl 
         (let [inarray [0.01 10.10 2.00001]
               foo (wrap inarray :float32)]
           @(enqueue-read foo ))))

(def foo (with-cl (wrap [0.01 10.10 2.00001] :float32)))
  

(with-cl queue)
(with-cl context)

(time (with-cl
         (let  [floatarray ;[0.01 10.10 2.00001]
                          (vec (map float [0.01 10.10 2.00001]))
                foo (wrap floatarray :float32)]
           (println (type @(enqueue-read foo )))
           (println (type floatarray))
           (println (type (nth @(enqueue-read foo ) 0)))
           (println (type (floatarray 0)))
           (type 0.01)
           (println (= (nth @(enqueue-read foo ) 0) (float 0.01)))
           (println (= (nth @(enqueue-read foo ) 0) (nth floatarray 0)))
           (= @(enqueue-read foo ) floatarray)
           )))

(version)

(type (float 1.3))
(type 1.3)

([1 23 4] 2)

(time (with-cl 
         (let [intfoo (wrap [10 9 8] :int32)]
           @(enqueue-read intfoo )
           (println (type @(enqueue-read intfoo )))
           (println (type (nth @(enqueue-read intfoo) 0)))
           (= @(enqueue-read intfoo) [10 9 8])
           )))
           
           (= [1 2 3] [1 2 3])




)


(def pp_openCL
  "
__kernel void foopp(
    __global float *liquidState1_a,
    __global float *liquidState1_b
    )
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
    liquidState1_b[gid] = liquidState1_a[gid] + 1.01;
}
  ")



(defn runpp []
(def pp_spindle (make_spindle))
(spindle_add_openCLsource! pp_spindle pp_openCL)
(start_spindle! pp_spindle)
(stop_spindle! pp_spindle)
)

(comment
(runpp)
)




(comment
  
  
  ;;OLD pperceptron code


;(use 'clojure.contrib.math)

(set! *warn-on-reflection* false)

;;;TODO: DONE keep perceptron config in a map?
;;TODO: STARTED  get liquids, but first pperceptrons to be objects.... so that we can hope to compose them later. 
;read http://thinkrelevance.com/blog/2009/08/12/rifle-oriented-programming-with-clojure-2.html
;read http://pramode.net/clojure/2010/05/26/creating-objects-in-clojure/

;;TODO:   Look at 'agents' aim to keep the openCL scope open and pass into it, so that buffers stay referencable.

; TODO (compile-program pp_openCL) should be on a closure with via a let!

; TODO breakout the creation function to be top level functions 


;TODO introduce a global queue to the one openCL thread?

(def pp_config 
      {:pperceptrons 2         ;Number of seperate paraller perceptrons, easy one can be considered a seperate estimator of a supervised learning signal being provided.
       :p_per_p 3              ;Number of perceptrons per parallel perceptron, must be odd, minimum is 3, 
       :alphas_per_p 3})       ;Number of synaptic links out of each perceptron to source data (these are the numer of weights that will be learned per each perceptron)

(def run_config
      {:learningloops 50       ;
       :epochs 10})


(defn size_of_alphas_array [app_config]
  "returns size of the alphas array from a parallep perceptron config"
     (* (:pperceptrons app_config)
        (:p_per_p app_config)
        (:alphas_per_p app_config)))


;(:p_per_p pp_config)
;(assoc run_config :epochs 15)
;(size_of_alphas_array pp_config)

(defn init_pp_alphas [app_config]
  "returns a deterministic random intial alphas weight vector"
   (doall
   (let [r (java.util.Random. 12345)]
     (loop [i 0 outlist ()]
       (if (= i (size_of_alphas_array app_config) )
         outlist
         (recur (inc i) (conj outlist (* (float (.nextInt r 100)) 0.1)) ))))))

;(init_pp_alphas pp_config)


(defn readout_float_buffer [whichbuffer start_read_at end_read_at]
    (let [buffer_data (^floats float-array (deref (enqueue-read whichbuffer [start_read_at end_read_at])))]
     (enqueue-barrier)(finish)
     (let [clj_arrayout (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at)))]
     (println clj_arrayout)
     clj_arrayout
     ))
)

(def pp_openCL
  "
__kernel void foopp(
    __global float *liquidState1_a,
    __global float *liquidState1_b
    )
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
    liquidState1_b[gid] = liquidState1_a[gid] + 1.01;
}
  ")

(defn make-pp [app_config]  
"function for creating parallep perceptrons AKA pp's"
    (let [pp 
          (atom  "this parallel perceptron needs to be initalised within with-cl first before it is used, initialise with :init_pp")
          ;;TODO a second atom for large responses???
          ]
      ;;Now define the set of function that can be applied to pp
      ;;TODO add a global pp iteration number
      {:init_pp! 
                  #(swap! pp (fn [_] {:alphas_array (wrap (init_pp_alphas app_config) :float32)
                                      :pps  (create-buffer (size_of_alphas_array app_config) :float32)  ;array of parralel perceptrons
                                      :ps   (create-buffer (size_of_alphas_array app_config) :float32)
                                      :running true
                                      :initialised true
                                      :terminated true
                                      :pps_clj_array []
                                      :current_instructions (atom (fn [x] (println "nothig set to do within pp")))
                                      }))
       :testflop_pp!
                                           #(if (:running @pp)
                                                (enqueue-kernel :foopp    ;;basic kernel for testsing
                                                 (size_of_alphas_array app_config)     ;;get the global sizes
                                                 (:alphas_array @pp)                   ;;the alphas_array of pp as first buffer to be passed to kernel...
                                                 (:alphas_array @pp)))
       :testflop_pp_fn!
                                           (fn [] (enqueue-kernel :foopp    ;;basic kernel for testsing
                                                   (size_of_alphas_array app_config)     ;;get the global sizes
                                                   (:alphas_array @pp)                   ;;the alphas_array of pp as first buffer to be passed to kernel...
                                                   (:ps @pp)
                                             ))
       ;;pp as second buffer to    TODO clean this up, this is just to see it work.....
       ;;TODO write the pp buffers here out of 'pp')
       ; :return_pp_orig @pp     ;;This is usless...
       :testflop_selfpass_fn!
                                           (fn [buffer_passed_in] (enqueue-kernel :foopp    ;;basic kernel for testsing
                                                (size_of_alphas_array app_config)     ;;get the global sizes
                                                (:alphas_array @pp)                   ;;the alphas_array of pp as first buffer to be passed to kernel...
                                                buffer_passed_in
                                             ))

       :return_pp_current   (fn [] @pp)
       :alphas_array_only   (fn [] (:alphas_array @pp))  ;;Could make this expose the 'current' array from a double buffer pair, thus allwoing easy 'infection' of data into a the neuron block entitiy
       :readout_pp          (fn [startread endread] (readout_float_buffer (:alphas_array @pp) startread endread))
       :readout_pp_clj      (fn [startread endread] (swap! pp (fn [x] (assoc x :pps_clj_array
                                                                                         (readout_float_buffer (:alphas_array @pp) startread endread)
                                                                                         ))))
       :stop_pp!             #(swap! pp (fn [x] (assoc x :running false)))    ;This is to let me stop or start execution of a pp...
       :start_pp!            #(swap! pp (fn [x] (assoc x :running true)))
       :nuke_pp!             #(swap! pp (fn [x] (do (println "nuking")
                                                    (release! (:alphas_array x))
                                                    (release! (:pps x))
                                                    (release! (:ps x))
                                                    "pp was nuked")))
       :ret_pps_clj_array       (fn [] (:pps_clj_array @pp))   ;promblem is timing, this array my be changed by another call...
       :set_instructions_here   (fn [] (:current_instructions @pp))
       :do_instructions         (fn [x] (@(:current_instructions @pp) x))
       }
       )
     ;;TODO!!!DOING IT instead of returning the pp itself, define the functions typically done within  with-cl, run program....
    ;(enqueue-kernel :flopliquid globalsize conectivity liquidState1_A liquidState1_B liquidState2_A liquidState2_B debug_infobuff connections)
)



;;;;;TEST HERE!!;;;;;TEST HERE!!;;;;;TEST HERE!!;;;;;TEST HERE!!;;;;;TEST HERE!!



;define an atom of opencl function to execute


;(@openCL_instructions_atom)

(defn start_pp_openCL_on_thread! [a_pp1 a_pp2]
(.start (Thread. (fn []
(with-cl (with-program (compile-program pp_openCL)
  ((a_pp1 :init_pp!))
  ((a_pp2 :init_pp!))
 ;;Put on loop
  (loop [k 200]
     (println "on step:" k)
     ((a_pp1 :do_instructions) a_pp1)     ;consider using this here: http://blog.marrowboy.co.uk/2011/10/21/worker-queues-in-clojure/
     ((a_pp2 :do_instructions) a_pp2)
     ;;To make this go into queues, does it make sense to have the pp here?.... better to just have a global queue?... make the queue the parameter... queue knowns for which pp it is doing work next...
     
     ;(run-opencl_functions a_pp)   ;; TODO DONE we want to be changing the set of instruction being done on this thread at run time...ie: another rifle loaded with functionality?
     (Thread/sleep 200)
  (if (= k 1) 1 (recur (dec k) )))

((a_pp1 :nuke_pp!))
((a_pp2 :nuke_pp!))
))))))

;;;Start work here:


(defn do_stuff0a [a_pp]
"this is thread unsafe since the "
(swap! ((a_pp :set_instructions_here)) (fn [_] 
      (fn [a_pp] (do
       nil)
      ))))

(defn do_stuff1a [a_pp]
(swap! ((a_pp :set_instructions_here)) (fn [_] 
      (fn [a_pp] (do
       ((a_pp :readout_pp) 0 3))
      ))))

(defn do_stuff2a [a_pp]
  (swap! ((a_pp :set_instructions_here)) (fn [_] 
      (fn [a_pp] (do
       ((a_pp :testflop_pp!))
       (enqueue-barrier)
       (finish)
       ((a_pp :readout_pp) 0 3)
       ((a_pp :testflop_pp!))
       ((a_pp :readout_pp_clj) 0 3))))))

(defn do_stuff3a_normalreturn [a_pp]
  (swap! ((a_pp :set_instructions_here)) (fn [_] 
      (fn [a_pp] (do
       ((a_pp :testflop_pp!))
       (enqueue-barrier)
       (finish)
       ((a_pp :readout_pp) 0 3)
       ((a_pp :testflop_pp!))
       ((a_pp :readout_pp_clj) 0 3)   ;;;What if this wrote answers to a map of answers, where the key was something unique to this one very call of this function. This function could then wait untill its response value show'ed up
                                      ;;;Danger is instructions would get lost, swaped over before they had a chance to get executed. Solution: block if instruction are not empty? Not very concurent....
     ))))
  ((a_pp :ret_pps_clj_array))   ; The work on the tight loop might/will not happen in time for the return to be as per this functions instructions.
)

;;TODO create a demo (defn do_stuff3 [a_pp1 a_pp2] ...) that will compose pp,  note, specific buffer level exposure to pp would be required? + dedicated openCL kernels


(def my_pp1 (make-pp pp_config))
(def my_pp2 (make-pp pp_config))

(start_pp_openCL_on_thread! my_pp1 my_pp2)
(Thread/sleep 2000)

(do_stuff1a my_pp1)
(do_stuff1a my_pp2)

(do_stuff2a my_pp1)
(do_stuff2a my_pp2)

(do_stuff0a my_pp1)
(do_stuff0a my_pp2)

(do_stuff3a_normalreturn my_pp1)      ;This is introducing a function that appears normal in that returns, but it does have side effects.


;mock (defn do_stuff_to_two_pp  [pp1 pp2]
;

((my_pp1 :ret_pps_clj_array))       ; gets back an array value.
((my_pp2 :ret_pps_clj_array))
((my_pp1 :set_instructions_here))   ; This is the atom holiding the current instructions

(count ((my_pp1 :ret_pps_clj_array)))




(quote ;Messing about with queues

(conj clojure.lang.PersistentQueue/EMPTY "foo")
(def aPersistentQueue clojure.lang.PersistentQueue/EMPTY)    ;;This could help above.... all requests are added to a queue
(peek (conj (pop (conj aPersistentQueue 1 2 3)) 42))
(pop aPersistentQueue)

(-> (clojure.lang.PersistentQueue/EMPTY)
          
          )
;(2 3)

(defn new-q [] (java.util.concurrent.LinkedBlockingDeque.))
(defn offer! 
  "adds x to the back of queue q"
  [q x] (.offer q x) q)
(defn take! 
  "takes from the front of queue q.  blocks if q is empty"
  [q] (.take q))
(def example-queue1 (new-q))
;; background thread, blocks waiting for something to print
(future (println ">> " (take! example-queue1))) 
;; the future-d thread will be unblocked and ">> HELLO WORLD" is printed.
(offer! example-queue1 "HELLO WORLD") 


;;;;;The return
;; THREAD 1
(def example-queue2 (new-q))
(def answer (promise))

(.start (Thread. 
 (do 
 (offer! example-queue2 {:q "how much?" :a answer})
 (println "The thread is waiting for an answer")
 (println @answer)
 (println "An answer got delivered") 
 )))   ; blocks, until... 

;; THREAD 2
(.start (Thread. 
   (do (let [msg (take! example-queue2)]
       (deliver (msg :a) "£3.650")
       "done"
       ))))

)

;; THREAD 1
; now unblocked, prints "£3.50"



;;TODO get data out of pp as a normal clojure return, based on an input, without the risk of world moving along... and without stopping other work done on the thread after the return... and with thread safety (if 100 thread call same function)
;;     Take the existing instructions, 'save them on the side'... or add to queue? Or have the whole thing on a queue?
;;     devote one openCL loop to the openCL computation requested, wait for one itteration to pass by on the openCL thread with the replaced instructions.
;;     aim: given clj array input, function return cljoutput,
;;     write output to clj out array (on internal atom), put original instruction 'back in',and return the clj array from the pp atom... Thus totaly hiding the fact work was done on a seperate thread and in openCL from the caller.
;;     Caller must not need to be within with-cl, pp will have to be 'running' on a start_pp_openCL_on_thread!




;mock    (defn ask_pp [a_pp clj_array_question] ....)  ->  clj array answer. 








;((my_pp :return_pp_current))
;(with-cl  ((my_pp :init_pp!)) ((my_pp :nuke_pp!)))



;;;;;TEST END HERE!!;;;;;TEST END HERE!!;;;;;TEST END HERE!!;;;;;TEST END HERE!!;;;;;TEST END HERE!!




















(defn open_cl_threaded_executor [] 2)
(defn init_openCL_queue)
(defn add_to_openCL_queue)


;(with-cl (def my_pp (make-pp pp_config)) )    ;;This create a parallel perceptrons, initialised with the given config..., held in the atom that is my_pp
;
;(with-cl  (def my_pp (make-pp pp_config)))
;(with-cl  ((my_pp :init_pp)))



(defn run_openCL! []
  "a bad way to do this"
(with-cl (with-program (compile-program pp_openCL)     ;TODO(compile-program pp_openCL) should be on a closure with via a let!
  (def my_pp (make-pp pp_config))
  ((my_pp :init_pp!))
  ((my_pp :testflop_pp!))
  (enqueue-barrier)
  (finish)
  ;;((my_pp :readout_pp) 0 3)
  ((my_pp :stop_pp!))
  ((my_pp :testflop_pp!))
  ;;((my_pp :readout_pp) 0 3)
)))










;(run_openCL_forever!)


;(Thread/sleep 100)   ; sleep so that there is enough time to create my_pp????
;((my_pp :stop_pp!))

;((my_pp :start_pp!))
;((my_pp :alphas_array_only))
;;moc for copying part between liquids

;(defn copybetweenliquid [liq1 liq2] 
;      (runsomeopenCLkernel ((liq1 :buff1)) ((liq1 :buff1))))

;:alphas_array_only

;;((my_pp :return_pp_current))





;;TODO may not need these initailly to develop as I can get same behaviour without starting threads
(defmacro forever [& body] 
  `(while true (do
                 ~@body
                (Thread/sleep 100)  ;;Adding this wait time makes this not suck up the whole CPU... it does only 1000 things can be passed in per second.
                )))

(defn RunThisForAVeryLongTime [fn]
(.start (Thread.
          (fn [] (forever (while (= true false)
           (do
             (println "I was waiting all that time")
             ;(swap! runInnerOpenCL (fn [_] false))
             (Thread/sleep 100)))))   ;This is bad, without the sleep it just heats up the CPU
             ;making it stop
)))


(quote
(run_openCL!)
((my_pp :stop_pp))
(run_openCL!)
((my_pp :start_pp))
(((my_pp :return_pp_current)) :running)
;;TODO, just need to add terminate, and put this on a loop on an agent?/thread?


((my_pp :return_pp_current))
(((my_pp :return_pp_current)) :alphas_array)
(((my_pp :return_pp_current)) :running)



(with-cl (with-program (compile-program pp_openCL)
   (def my_pp4 (make-pp pp_config))
   ((my_pp4 :init_pp))
   ((my_pp4 :readout_pp) 0 10)
   ((my_pp4 :testflop_pp_fn))
   ((my_pp4 :readout_pp) 0 10)
   ((my_pp4 :testflop_selfpass_fn)  (((my_pp4 :return_pp_current)) :alphas_array))
   ((my_pp4 :readout_pp) 0 10)
   ((my_pp4 :testflop_selfpass_fn)  (((my_pp4 :return_pp_current)) :alphas_array))
   (enqueue-barrier)
   (finish)
   ((my_pp4 :readout_pp) 0 10)
))


(with-cl 
   ((my_pp :init_pp))
   ((my_pp :readout_pp) 0 10)
   )



(with-cl 
  (def my_pp (make-pp pp_config))
  (def my_pp2 (make-pp pp_config))
  ((my_pp :init_pp))
  ((my_pp2 :init_pp))
  ;;((my_pp :testflop_pp))
  
  ((my_pp :readout_pp) 0 3)
  (with-program (compile-program pp_openCL)
       ((my_pp :testflop_pp))
       (enqueue-barrier)
       (finish)
       )
  ((my_pp :readout_pp) 0 3)
  ((my_pp2 :readout_pp) 0 3)

)


)





;;((my_pp :init_pp))

;; TODO between the two with-cl contexts, are we looking at the same memory space...very same thing???   -- A: sames issues as before, you loose it as you loose the with-cl ,with-program scope 
;; NOTE , many initialisation eat up CL resources


;(with-cl 
;   (with-program (compile-program sourceOpenCL2)
;       ((my_pp :testflop_pp))
;       (enqueue-barrier)
;       (finish)
;       )
;   )





;;TODO... result is nill.... but was any work done?  Prove the buffer works, Test performance with this rifle idiom...   
;;TODO explore how compositon of pp and liquids could be achived...with this mechanisim...




;(def my_pp2 (make-pp pp_config))

;(with-cl  ((my_pp :init_pp)))

;(println my_pp)
;(println my_pp2)



;(defn run_liquid! [globalsizeZ connections OpenCLSourceToUse]
;  (with-cl
;     (with-program (compile-program OpenCLSourceToUse)
;          (if @flop_liquid_status (flop_liquid! globalsizeZ connections liquidState1_a liquidState1_b liquidState2_a liquidState2_b debug_infobuff))
;        )
;      "Terminating pp executon"
;   ))




;(defn init_ppons! [app_config]
;  "Initialises the parallel perceptrons based on a config"
;    (def {:alphas_array (wrap (init_pp_alphas app_config) :float32))
;    (def liquidState1_b  (create-buffer globalsizeZ :float32))  ;Spike activatio0n buffer B of the state double buffer
;    (def liquidState2_a  (wrap (random_liquid_seedZ globalsizeZ 1.0) :float32)) ;Activation potential buffer A of the state double buffer
;    (def liquidState2_b  (create-buffer globalsizeZ :float32))  ;Activation potential buffer B of the state double buffer
;    (enqueue-barrier)(finish)
;    
;    ;(swap! init_liquid_status (fn [_] false))
;    )

;(defn funkymapfun [foo]
;  {:a (+ foo 5)})
;(:a (funkymapfun 10))

)


)