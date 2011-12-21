(ns LudoClojure.liquid
  (:require calx)
  (:gen-class))

(use 'calx)
;(use 'clojure.contrib.math)

(set! *warn-on-reflection* false)

(def globalsize (* 64 64 64))  ;Total size of liquid
(def connections 5)                 ;Number of neurons each neuron should connect to, a double connection is more and more likely with the size...
(def opencl_loops 50)
;;Note
;;buffers 'a' are from t-1. buffers 'b' are t bufferrs (those that need to be computed now)
(def sourceOpenCL
  "

__kernel void flopliquid(
    __global int *conectivity,
    __global float *liquidState1_a,     
    __global float *liquidState1_b,
    __global float *liquidState2_a,     
    __global float *liquidState2_b,
    const int connections
        )   
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);

    int iatom = 0;
    int random_value = 0;
    int inhibition_chooser = 0;
    int gid_to_read = 0;
    float liquidState1 = liquidState1_a[gid] ;   //read in the spiking state of the current neuron at t-1
    float liquidState2 = liquidState2_a[gid] ;   //read in the synaptic potential from the t-1 buffer
    float liquidState1_orig = liquidState1;       //taking stock of the spike strength before we start changing it       (looking to optimise away unnecessary writes)
    float liquidState2_orig = liquidState2;      //taking stock of the activation potential before we start changing it (looking to optimise away unnecessary writes)
    //__local float mylocalBuffer[1024];  // could create a local buffer so that the write are coelesed __local float localBuffer[1024];  as per here http://stackoverflow.com/questions/2541929/how-do-i-use-local-memory-in-opencl

    uint m_z = conectivity[gid];  //Randomnumber generator step: random number initial seed, this is needed per each kernel execution so that random number created are all different, else each kernel would produce same set of randoms, also allows for conectivity mutations
    uint m_w = gid;               //Randomnumber generator step: secondary seed

    for(iatom = 0; iatom < connections; iatom+=1 ) {
        m_z = 36969 * (m_z & 65535) + (m_z >> 16);    //Randomnumber generator step
        m_w = 18000 * (m_w & 65535) + (m_w >> 16);    //Randomnumber generator step
        random_value = ((m_z << 16) + m_w);           //Randomnumber generator step:The large rundom number
        inhibition_chooser = random_value % 100;      //Randomnumber generator step:Random number to determine inhibitory vs exitory connection
        gid_to_read = random_value % gsize;           //Randomnumber generator step which nuron to read from     TODO add locality via xyz space here... Note this is the only place that needs it?

        //Note: literature states that the neuron is inhibitory or exitory, not the synapse, so this model is wrong, would instead have to load in a ranom +-1 arrey (idealy persist it beteween runs)

        if (inhibition_chooser >= 11) {
          liquidState2 = liquidState2 + (liquidState1_a[gid_to_read] * 0.212);    //action potential getting subtracted or added based on spiking state of connected neurons
          }
        else  {
                if (liquidState2 < -1.0) {
                //Do nothing,  potential is as depressed as it can get, save yourself a global GPU RAM read
                }
                else  {
                liquidState2 = liquidState2 - (liquidState1_a[gid_to_read] * 0.0005);   //action potential getting subtracted or added based on spiking state of connected neurons
                }
               }
        }
        //TODO add neuron activation potential 
        // Any writing out of bounds of an array corrupts memory further afield!!!
        // Note: ANY writing to the same location causes a catastrophic result fail, aka race condition


// Note
// liquidState1  - spike strength
// liquidState2  - activation potential
//Idea: for debug/speed optimisation, we could see which case gets hit the most often, and put loop in that order

    if (liquidState1 < 0.1  && liquidState2 > 1.0 ) {   //The neuron went over the action potential limit and has not just fired (absolute refractory requirement), hence it will now fire 
      liquidState1_b[gid] = 1.0;                        // Nuron is firing spike strength goes to 1
      liquidState2_b[gid] = -0.2;                       // Nuron is firing, the potential goes into 'relative refractory period' where it is more unlikely to fire, neuron will recover from negative activation potential back to zero on it's now.
      }
     // adding logic to preven writes back to global memory if the neuron has finished firing and/or it's activation potential is stable at zero (hence nothig to update)
    else if (liquidState1 == 0.0 && liquidState2_orig == 0.0 && liquidState2 == 0.0)  { //Neuron is sleeping, and no new potential delta arrived this tick, neuron has nothing to do.
      //Do nothing, save yourselft a write to global GPU ram
      }
    else if (liquidState1 < 0.01 && liquidState2_orig == 0.0 && liquidState2 == 0.0)  { //Neuron is at rest, and no new potential delta arrived this tick, put neuron to sleep.
      liquidState1_b[gid] = 0.0;
      }
    else if (liquidState1 == 0.0 && liquidState2 <= -0.025)  {                       //Action potential was depressed below zero, it will thus be held at zero (OLD NOTE: could use this to detece a neuron that will never fire as it's constantly in hibited, could use this as a triger to reconfigure..... 
      // liquidState1_b[gid] = liquidState1 * 0.5; No need to do this write
      liquidState2_b[gid] = liquidState2 + 0.025;
      }
    else if (liquidState1 == 0.0 && liquidState2 > -0.025 && liquidState2 < 0.01)  {                       //Action potential was depressed below zero, it will thus be held at zero (could use this detece a neuron that will never fire as it's constantly in hibited, could use this as a triger to reconfigure..... 
      liquidState2_b[gid] = 0.0;   //Putting the potential to sleep mode at zero.
      }
   else if (liquidState2 < -0.025)  {                       //Action potential was depressed below zero, it will thus be held at zero (could use this detece a neuron that will never fire as it's constantly in hibited, could use this as a triger to reconfigure..... 
      liquidState1_b[gid] = liquidState1 * 0.5;
      liquidState2_b[gid] = liquidState2 + 0.025;
      }
    else  {
      liquidState1_b[gid] = liquidState1 * 0.5;   //Neuron not firing, it's spike strength decays quickly
      liquidState2_b[gid] = liquidState2 * 0.99;  //Neuron not firing, it's activation potential slowly decays (but 'remembers' recent activity via the potential
      }

    //liquidState1_b[gid] = (1.0 / (1.0 + exp( -1.0 * liquidState1))) +0.002;
    
}
  ")


;      (time (run_liquid! globalsize connections sourceOpenCL)) (show_diagnostics)


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


;;(float-array 16 '(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16))
;(for [i (range (* 16))] (float (rand)))

(def random_liquid_seed (doall (for [i (range (* globalsize))] (float (rand)))))
;(count random_liquid_seed)

(def init_liquid_status (atom true))
(defn init_liquid! [globalsize]
  "Initialises the liquid states"
    (def conectivity     (create-buffer globalsize :int32))    ;A random used to influence the random number seed, so that a nuron can be rerolled if deemed poorly connected 
;    (def liquidState1_a  (create-buffer globalsize :float32))  ;Spike activation buffer A of the state double buffer
    (def liquidState1_a  (wrap random_liquid_seed :float32))
    (def liquidState1_b  (create-buffer globalsize :float32))  ;Spike activation buffer B of the state double buffer
    (def liquidState2_a  (create-buffer globalsize :float32))  ;Activation potential buffer A of the state double buffer
    (def liquidState2_b  (create-buffer globalsize :float32))  ;Activation potential buffer B of the state double buffer
    ;(swap! init_liquid_status (fn [_] false))
    )


(def flop_liquid_status (atom true))
(defn flop_liquid! [globalsize connections liquidState1_A liquidState1_B liquidState2_A liquidState2_B]
    (enqueue-kernel :flopliquid globalsize conectivity liquidState1_A liquidState1_B liquidState2_A liquidState2_B connections)
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


(defn floatround [s n] 
     (.setScale (bigdec n) s java.math.RoundingMode/HALF_EVEN))

(def readout_liquid_status (atom true))
(defn readout_liquid! [whichliquidbuffer]
    (let [liquid_data (^floats float-array (deref (enqueue-read whichliquidbuffer [0 20000])))]
     (println 
                     (map  (fn [x] 
                     ;;; ;(floatround 0 (nth liquid_data x))
                      (if (>= (nth liquid_data x) 0.04) 1 0)
                      )(range 0 64))  
              "total on:" (reduce + 0 (map (fn [x] 
                      ;(floatround 0 (nth liquid_data x))
                      (if (>= (nth liquid_data x) 0.04) 1 0)
                      )(range 0 20000))   )
    ))
    ;(swap! readout_liquid_status (fn [_] false))
    ;(swap! readout_liquid_status (fn [_] true))
)


(defn run_liquid! [globalsize connections OpenCLSourceToUse]
  (with-cl
   (if @init_liquid_status (init_liquid! globalsize))
     (def checkpoint1_start (. System (nanoTime)))
      ;Main with-cl loop starts here
      (with-program (compile-program OpenCLSourceToUse)
        (loop [k opencl_loops]
          (if @readout_liquid_status (readout_liquid! liquidState1_a))
          (if @flop_liquid_status (flop_liquid! globalsize connections liquidState1_a liquidState1_b liquidState2_a liquidState2_b))
          ;(if @readout_liquid_status (readout_liquid! liquidState1_b))
          (if @flop_liquid_status (flop_liquid! globalsize connections liquidState1_b liquidState1_a liquidState2_b liquidState2_a))
          
        (Thread/sleep 0)
        (if (= k 1) nil (recur (dec k))))
        )
      (def checkpoint1_end (. System (nanoTime)))
      ;Main with-cl loop ends here
    ;(println "to infinity, and beyond:" k)
   ))

(defn show_diagnostics []
(println "Flops per second  : "(/ (* opencl_loops 2) (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0)))
(println "Bil ops per second: "(/ (/ (* globalsize connections opencl_loops 2) (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0)) 1000000000))
(println "Number of synapses: "(* globalsize connections)   "  Number of neurons:" globalsize )
)

(def globalsize (* 64 64 64))  ;Total size of liquid
(def connections 5)                 ;Number of neurons each neuron should connect to, a double connection is more and more likely with the size...
(def opencl_loops 50)

;       (time (run_liquid! globalsize connections sourceOpenCL)) (show_diagnostics)


;TODO, auto benchmark over a space of options, 2 dimentions, intervals over which to test, increments , measure to use)... injecting into with-cl as needed... 

;Accurate Time (/ (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0) opencl_loops)
;;Billion operations per second...
 ;     (total operations                       ) (total seconds                                        )  billion   )


(println "got to checkpoint 1")

;run this:
;;TODO, be able to 'run' things within a threaded with-cl (that's in a loop), by running a function....
;;TODO change the definiton of that runs within the winth-cl, while it's running by redefining the call being made.... via macros or atoms
;;TODO: DONE: Have a loop that loops over each statement within with-cl, quickly checking if it should be executed or not, if yes, executing, seting state back to 'done'


