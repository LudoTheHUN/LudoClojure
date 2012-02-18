(ns LudoClojure.pperceptron
  (:require calx)
  (:gen-class))

(use 'calx)
(use 'LudoClojure.liquid)
;(use 'clojure.contrib.math)

(set! *warn-on-reflection* false)

;;;TODO: DONE keep perceptron config in a map?
;;TODO: STARTED  get liquids, but first pperceptrons to be objects.... so that we can hope to compose them later. 
;read http://thinkrelevance.com/blog/2009/08/12/rifle-oriented-programming-with-clojure-2.html
;read http://pramode.net/clojure/2010/05/26/creating-objects-in-clojure/

;;TODO:   Look at 'agents' aim to keep the openCL scope open and pass into it, so that buffers stay referencable.

; TODO (compile-program pp_openCL) should be on a closure with via a let!

; TODO breakout the creation function to be top level functions 

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
     (println (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at))))
     buffer_data
     )
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
          (atom 
                       ; {:alphas_array (wrap (init_pp_alphas app_config) :float32)
                       ;  :pps  (create-buffer (size_of_alphas_array app_config) :float32)  ;array of parralel perceptrons
                       ;  :ps   (create-buffer (size_of_alphas_array app_config) :float32)}       ;array of perceptrons
                       "this parallel perceptron needs to be initalised first before it is used, initialise with :init_pp"
                        )
          ]
      ;;Now define the set of function that can be applied to pp
      {:init_pp! 
                  #(swap! pp (fn [_] {:alphas_array (wrap (init_pp_alphas app_config) :float32)
                                      :pps  (create-buffer (size_of_alphas_array app_config) :float32)  ;array of parralel perceptrons
                                      :ps   (create-buffer (size_of_alphas_array app_config) :float32)
                                      :running true
                                      :initialised true
                                      :terminated true
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
       :stop_pp!             #(swap! pp (fn [x] (assoc x :running false)))    ;This is to let me stop or start execution of a pp...
       :start_pp!            #(swap! pp (fn [x] (assoc x :running true)))
       :nuke_pp!             #(swap! pp (fn [x] (do (println "nuking")
                                                    (release! (:alphas_array x))
                                                    (release! (:pps x))
                                                    (release! (:ps x))
                                                    "pp was nuked")))
       }
       )
     ;;TODO!!!DOING IT instead of returning the pp itself, define the functions typically done within  with-cl, run program....
    ;(enqueue-kernel :flopliquid globalsize conectivity liquidState1_A liquidState1_B liquidState2_A liquidState2_B debug_infobuff connections)
)



;;;;;TEST HERE!!;;;;;TEST HERE!!;;;;;TEST HERE!!;;;;;TEST HERE!!;;;;;TEST HERE!!


(defn run_openCL_steps [a_pp] 
  ((a_pp :testflop_pp!))
  (enqueue-barrier)
  (finish)
  ((a_pp :readout_pp) 0 3)
  ((a_pp :testflop_pp!))
  ((a_pp :readout_pp) 0 3)
  )

(def my_pp (make-pp pp_config))

(defn start_pp_openCL_on_thread! [run-opencl_functions a_pp]
(.start (Thread. (fn []
(with-cl (with-program (compile-program pp_openCL)
  ((a_pp :init_pp!))
 ;;Put on loop
  (loop [k 20]
     (println "on step:" k)
     
     (run-opencl_functions a_pp)   ;; TODO we want to be changing the set of instruction being done on this thread at run time...
     
     (Thread/sleep 1)
  (if (= k 1) 1 (recur (dec k) )))

((a_pp :nuke_pp!))
))
))))


;(start_pp_openCL_on_thread! run_openCL_steps my_pp)

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


