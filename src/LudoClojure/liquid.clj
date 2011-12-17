(ns LudoClojure.liquid
  (:require calx)
  (:gen-class))

(use 'calx)
;(use 'clojure.contrib.math)

(set! *warn-on-reflection* false)



(def sourceOpenCL
  "

__kernel void flopliquid(
    __global int *conectivity,
    __global float *liquidState1_a,
    __global float *liquidState1_b,
    const int connections
    )
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);

    uint iatom = 0;
    uint random_value = 0;
    uint inhibition_chooser = 0;
    uint gid_to_read = 0;
    float liquidState1 = 0.0 ;


    uint m_z = conectivity[gid];  //random number initial seed, this is needed per each kernel execution so that random number created are all different, else each kernel would produce same set of randoms
    uint m_w = gid;

    for(iatom = 0; iatom < connections; iatom+=1 )
    {
        m_z = 36969 * (m_z & 65535) + (m_z >> 16);    //Randomnumber generator step
        m_w = 18000 * (m_w & 65535) + (m_w >> 16);    //Randomnumber generator step
        random_value = ((m_z << 16) + m_w);       //Randomnumber generator step:The large rundom number
        inhibition_chooser = random_value % 100;  //Randomnumber generator step:Random number to determine inhibitory vs exitory connection
        gid_to_read = random_value % gsize;       //Randomnumber generator step which nuron to read from     TODO add locality via xyz space here... Note this is the only place that needs it?

        if (inhibition_chooser <= 50) {
        liquidState1 = liquidState1 - liquidState1_a[gid_to_read];
        }
        if (inhibition_chooser > 50) {
        liquidState1 = liquidState1 + liquidState1_a[gid_to_read];
        }

        //TODO add neuron activation potential 
        // Any writing out of bounds of an array corrupts memory further afield!!!
        // Note: ANY writing to the same location causes a catastrophic result fail, aka race condition

    }
 
    liquidState1_b[gid] = (1.0 / (1.0 + exp( -1.0 * liquidState1))) +0.0000001;
 
}
  ")
;      (run_liquid! globalsize connections)


(def sourceOpenCL2
  "
__kernel void flopliquid(
    __global int *conectivity,
    __global float *liquidState1_a,
    __global float *liquidState1_b,
    const int connections
    )
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);
    liquidState1_b[gid] = liquidState1_a[gid];
}
  ")



(def globalsize (* 64 64))  ;Total size of liquid
(def connections 10)                  ;Number of neurons each neuron should connect to, a double connection is more and more likely with the size...
(def opencl_loops 100)


;        (run_liquid! globalsize connections)


(def init_liquid_status (atom true))
(defn init_liquid! [globalsize]
  "Initialises the liquid states"
    (def conectivity     (create-buffer globalsize :int32))    ;A random used to influence the random number seed, so that a nuron can be rerolled if deemed poorly connected 
    (def liquidState1_a  (create-buffer globalsize :float32))  ;Spike activation buffer A of the state double buffer
    (def liquidState1_b  (create-buffer globalsize :float32))  ;Spike activation buffer B of the state double buffer
    ;(swap! init_liquid_status (fn [_] false))
    )


(def flop_liquid_status (atom true))
(defn flop_liquid! [globalsize connections liquidState1_a liquidState1_b]
    (enqueue-kernel :flopliquid globalsize conectivity liquidState1_a liquidState1_b connections)
             ;(println "done enqueue kernel3, global size is: " globalsize " conectivity is:" connections)
    (enqueue-barrier)
    (finish)
    ;(release! conectivity)
    ;(release! liquidState1_a)
    ;(release! liquidState1_b)
    ;(enqueue-barrier)
    ;(finish)
    ;(swap! flop_liquid_status (fn [_] false))
)

(def readout_liquid_status (atom true))
(defn readout_liquid! [whichliquid]
    (let [liquid_data (^floats float-array (deref (enqueue-read whichliquid [0 16])))]
     (println (map  (fn [x] (nth liquid_data x))(range 0 5)))
    )
    ;(swap! readout_liquid_status (fn [_] false))
    ;(swap! readout_liquid_status (fn [_] true))
)


(defn run_liquid! [globalsize connections]
  (with-cl
   (if @init_liquid_status (init_liquid! globalsize))
     (def checkpoint1_start (. System (nanoTime)))
      ;Main with-cl loop starts here
      (with-program (compile-program sourceOpenCL)
        (loop [k opencl_loops]
          (if @flop_liquid_status (flop_liquid! globalsize connections liquidState1_a liquidState1_b))
          (if @readout_liquid_status (readout_liquid! liquidState1_a))
          ;(if @readout_liquid_status (readout_liquid! liquidState1_b))
          (if @flop_liquid_status (flop_liquid! globalsize connections liquidState1_b liquidState1_a))
          (if @readout_liquid_status (readout_liquid! liquidState1_b))
          ;(if @readout_liquid_status (readout_liquid! liquidState1_a))
        (Thread/sleep 0)
        (if (= k 1) nil (recur (dec k))))
        )
      (def checkpoint1_end (. System (nanoTime)))
      ;Main with-cl loop ends here
    ;(println "to infinity, and beyond:" k)
   ))



(def globalsize (* 64 64 64 4))  ;Total size of liquid
(def connections 40)                 ;Number of neurons each neuron should connect to, a double connection is more and more likely with the size...
(def opencl_loops 20)

(time (run_liquid! globalsize connections))
(println "Flops per second  : "(/ (* opencl_loops 2) (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0)))
(println "Bil ops per second: "(/ (/ (* globalsize connections opencl_loops 2) (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0)) 1000000000))



;TODO, auto benchmark over a space of options, 2 dimentions, intervals over which to test, increments , measure to use)... injecting into with-cl as needed... 

;Accurate Time (/ (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0) opencl_loops)
;;Billion operations per second...
 ;     (total operations                       ) (total seconds                                        )  billion   )


(println "got to checkpoint 1")




;run this:



;;TODO, be able to 'run' things within a threaded with-cl (that's in a loop), by running a function....
;;TODO change the definiton of that runs within the winth-cl, while it's running by redefining the call being made.... via macros?

;;TODO: DONE: Have a loop that loops over each statement within with-cl, quickly checking if it should be executed or not, if yes, executing, seting state back to 'done'










(quote
  (loop [k 10]
    (println "to infinity, and beyond:" k)
    (if (= k 1) nil (recur (dec k))))

    (enqueue-kernel :flopliquid globalsize conectivity liquidState1_a liquidState1_b connections)
             (println "done enqueue kernel1")
    (enqueue-barrier)
    (finish)
    (release! conectivity)
    (release! liquidState1_a)
    (release! liquidState1_b)
    (enqueue-barrier)
    (finish)
    
    
    (enqueue-kernel :flopliquid globalsize conectivity liquidState1_b liquidState1_a connections)
             (println "done enqueue kernel2")
    (enqueue-barrier)
    (finish)
    (release! conectivity)
    (release! liquidState1_a)
    (release! liquidState1_b)
    (enqueue-barrier)
    (finish)
    
;(print liquidState1_a)
)


;; ..
;; prcess will consume and produce data,
;; how should this be comunicated in and out (atoms)

;;  TODO Q: How to have items within with-cl columicate back to top level... only atoms, do these atoms need to exits before had... do we want an 'object'

(quote "Trying to externalise the buffer definition... no lucl"
(defn init_liquid! [globalsize]
  "Initialises the liquid states"
  (with-cl
    (def conectivity     (create-buffer globalsize :int32))    ;A random used to influence the random number seed, so that a nuron can be rerolled if deemed poorly connected 
    (def liquidState1_a  (create-buffer globalsize :float32))  ;Spike activation buffer A of the state double buffer
    (def liquidState1_b  (create-buffer globalsize :float32))  ;Spike activation buffer B of the state double buffer
    ))

(defn init_liquid-atom! [globalsize]
  "Can not use this because everything has to be define within the with-cl context that will actually do the work... Initialises the liquid states"
  (def conectivity (atom 1))
  (def liquidState1_a (atom 1))
  (def liquidState1_b (atom 1))
  (with-cl
    (swap! conectivity     (fn [_] (create-buffer globalsize :int32)))    ;A random used to influence the random number seed, so that a nuron can be rerolled if deemed poorly connected 
    (swap! liquidState1_a  (fn [_] (create-buffer globalsize :float32)))  ;Spike activation buffer A of the state double buffer
    (swap! liquidState1_b  (fn [_] (create-buffer globalsize :float32)))  ;Spike activation buffer B of the state double buffer
    ) (println init_liquid-atom! "done"))
)

(quote   "Test statements that create openCL buffers"
(with-cl
  (swap! buffer_atom  (fn [_] (create-buffer (* 16) :int32  ))))
(with-cl
  (def buffer_def  (fn [_] (create-buffer (* 16) :int32  ))))

(with-cl
  (def buffer_def_create  (create-buffer (* 16) :int32  )))

(def buffer_def_clean  (create-buffer (* 16) :int32  ))

(in-ns 'LudoClojure.timelooper1.timelooper1)
)


