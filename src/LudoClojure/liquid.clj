(ns LudoClojure.liquid
  (:use [calx])
  (:require [LudoClojure.opencl-utils :as cl-utils])
  (:require [LudoClojure.utils :as utils])
  (:gen-class))

;(use 'calx)
;(use 'clojure.contrib.math)
;(use 'LudoClojure.liquid)
(set! *warn-on-reflection* true)

(quote
LSM TODOs

new neuron model as per "http://www.izhikevich.org/publications/spikes.htm"




DONE Use quil to visualise state
make parameters optional so that they can be hand tweaked to something good
make parameters setable within quil visualisation
create machanisim to correctly inject information
attach pp readout and learn an output
develop tests to show simple memory
develop tests to show generalisation
;add 2D topology
;add 3D topology

DONE Get modeled neurons more realistic as per calc model
DONE Note, this could take forever, need tools to manage this... Tune parameters to somethig realistic, spike should be much more then typical potential
NOGO Look at storm, we need to create many liquids on the fly, just by passing a name of this liquid, create a DSL for creating them... what would be an idomatic way of doing this? Answer defrecord and defprotocol
DONE Thing about output neurons, pdelta rule, parallel perceptrons, their GPU implementation....Need to create many on the fly.... DSL again? Answer pp done properly.
DONE implement p-delta rule
Set up a test case for testing, test data to inject... real vs procedurally created?
)




(cl-utils/add_kernel_specs cl-utils/opencl_kernels
{:flopLiquid {
     :desc "flops a liquid one time tick foward"
     :postion 11
     :body "
__kernel void flopLiquid(
    __global int *conectivity,
    __global float *liquidState1_a,
    __global float *liquidState1_b,
    __global float *liquidState2_a, 
    __global float *liquidState2_b,
    __global int *debug_infobuff,
    const int connections
        )   
{
    int gid = get_global_id(0);                  //This will a number between 0 and get_global_size-1.
    int gsize = get_global_size(0);              //This will be the global size.

    int iatom = 0;                               //This is used to loop over all synapses in the neuron is connected to
    int random_value = 0;                        //This will be random value that will be used to procedurally create the synapse characterystics
    int inhibition_chooser = 0;                  //This will use the random value to also decide if this connection in inhibitory or exitory, note that coupling with position may lead to a non symetric system.
    int gid_to_read = 0;                         //This will the neuron to read from for this synapse, needs work to make it local.
    float liquidState1 = liquidState1_a[gid] ;   //Read in the Action Potential (aka: spike strength) of the current neuron at t-1.
    float liquidState2 = liquidState2_a[gid] ;   //Read in the Activation Potential (aka: least action potential needed to start a spike) from the t-1 buffer.
    float liquidState1_orig = liquidState1;      //Taking stock of the Action Potential before we start changing it       (looking to optimise away unnecessary writes later if we have no change.)
    float liquidState2_orig = liquidState2;      //Taking stock of the Activation Potential before we start changing it (looking to optimise away unnecessary writes)
    //__local float mylocalBuffer[1024];         //Future optimisation: could create a local buffer so that the write are coelesed __local float localBuffer[1024];  as per here http://stackoverflow.com/questions/2541929/how-do-i-use-local-memory-in-opencl

    uint m_z = conectivity[gid];                 //Randomnumber generator step: random number initial seed, this is needed per each kernel execution so that random number created are all different, else each kernel would produce same set of randoms, also allows for conectivity mutations (future research area), clojure creates this random number deterministically so executions are reproducable.
    uint m_w = 1;                                //Randomnumber generator step: Note, can not be gid because random number generator needs non zero initial values



if (liquidState1 > liquidState2) {               //If we have a spike, then.... very little to do...
   liquidState1_b[gid] = 10.0;                      //Action Potential getting set to the spike level
   liquidState2_b[gid] = 100.0;                     //Activation Potential getting set to the spike level, making another spike very unlikely untill it decays off
}
else  {
    for(iatom = 0; iatom < connections; iatom+=1 ) {
        m_z = 36969 * (m_z & 65535) + (m_z >> 16);    //Randomnumber generator step: As per http://en.wikipedia.org/wiki/Random_number_generation
        m_w = 18000 * (m_w & 65535) + (m_w >> 16);    //Randomnumber generator step:
        random_value = ((m_z << 16) + m_w);           //Randomnumber generator step: The large 32bit random number generated by the process for this synapse.
        inhibition_chooser = abs(random_value % 100);      //Randomnumber generator step: Random number to determine inhibitory vs exitory connection
        gid_to_read = abs(random_value % gsize);           //Randomnumber generator step: which nuron to read from     TODO: add locality via xyz space here... Note this is the only place that needs to know the liquid conectivity topology, this can also be created procedurally, as a function of gid.

        //Note: LSM literature states that the neuron is inhibitory or exitory, not the synapse, so this model is wrong, would instead have to load in a random +1,-1 int arrey with correct distribution, or use signage of an existing vlaue, eg: liquidState1, assuming signage can never change during normal functioning  (idealy create procedurally to be able to replicate exactly.)

//Major refactor Jan 2012

        if (inhibition_chooser >= 17) {
          liquidState1 = liquidState1 + (liquidState1_a[gid_to_read] * 0.14);    //action potential getting subtracted or added based on Action Potential of connected neurons
          }
        else  {
          liquidState1 = liquidState1 - (liquidState1_a[gid_to_read] * 0.31);    //this is inhibition, action potential getting subtracted or added based on Action Potential of connected neurons
          }
         }   // Closes the for loop over iatom , aka synapses

     // TODO: Speed optimisations possible here by preventing writes when values are not changing, (when just at the asimptotes).

     liquidState1_b[gid] = liquidState1 * 0.1;                                  // Writing out the Action Potential , which is reduced by the decay rate    TODO: could make this a parameter
     liquidState2_b[gid] = ((liquidState2 - 1.0) * 0.6 ) + 1.0;                 // Writing out the Activation Potential, Decaying it by 0.6, asymptoting it to 2.0.   TODO: long term other slower neuron dynamics can be added here by making the decay rate and assimptote represent neuronal adaptations to its typical activation rates.
                                                                                //TODO: Maas made the decay rates different if the neuron was exitory vs inhibitory, that's for another refactor...

}  //Closing the non spike firing else

        // Any writing out of bounds of an array corrupts memory further afield!!!
        // Note: ANY writing to the same location twice causes a catastrophic result fail, aka race condition

//debug_infobuff[gid]= random_value;
debug_infobuff[gid]= gid_to_read;

}
"
}})



(def globalsize (* 64 64))  ;Total size of liquid
(def connections 5)         ;Number of neurons each neuron should connect to, a double connection is more and more likely with the size...
(def opencl_loops 50)


;(println utils/opencl_kernels "boom")
(keys @cl-utils/opencl_kernels)

;(cl-utils/opencl_env_compileprogs cl-utils/opencl_env (cl-utils/get_openCL_from_kernels cl-utils/opencl_kernels))
(cl-utils/opencl_compile_default)

(:progs @cl-utils/opencl_env)

(defprotocol LiquidProtocol
  "Protocol for interacting with a liquid of a liquid state machine."
(flip [liquid] "flips the flipper bit all double buffer state entries relate to")
(flop    [liquid] "tick the liquid by one time step")
(inject  [liquid input] "inject information into liquid")
(readoff_speced [liquid spec] "read the liquid state as specified by the spec")
(readoff [liquid] "read the full liquid state")
)


(defrecord LiquidRecord [liquid_opencl_env
                         liquid_queue
                         flipper      ;;atom that defines the double buffer behaviour
                         
                         liquidState1_a_buf
                         liquidState1_b_buf
                         liquidState2_a_buf
                         liquidState2_b_buf
                         conectivity_buf
                         debug_infobuff_buf
                         connections
                         liquidsize
                        ])

(extend-type LiquidRecord LiquidProtocol
(flip [liquid] "flips the flipper bit all double buffer state entries relate to"
  (let [flipper (:flipper liquid)]
    (swap! flipper (fn [x] (not x)))))

(flop [liquid]
 (let [flipper (:flipper liquid)
       flipbit @flipper]
  (do (flip liquid)   ;;TODO think if this should no be on it's own in a dedicated protocol function to make the flip explicit outside
  ;(do (swap! flipper (fn [x] (not x)))   ;;TODO think if this should no be on it's own in a dedicated protocol function to make the flip explicit outside
    (lg_enqueue-kernel ((:liquid_queue liquid) @(:liquid_opencl_env liquid)) (:progs @(:liquid_opencl_env liquid))
                     :flopLiquid
                     (:liquidsize liquid)   ;;globalsize
                     (:conectivity_buf liquid)
                     (if flipbit (:liquidState1_a_buf liquid) (:liquidState1_b_buf liquid))    ;;double buffer logic
                     (if flipbit (:liquidState1_b_buf liquid) (:liquidState1_a_buf liquid))
                     (if flipbit (:liquidState2_a_buf liquid) (:liquidState2_b_buf liquid))
                     (if flipbit (:liquidState2_b_buf liquid) (:liquidState2_a_buf liquid))
                    ;(:liquidState1_a_buf liquid)
                    ;(:liquidState1_b_buf liquid)
                    ;(:liquidState2_a_buf liquid)
                    ;(:liquidState2_b_buf liquid)
                    (:debug_infobuff_buf liquid)
                    (:connections liquid)
                  ))))

(inject  [liquid input]
 "input is a vector or an  :float32-le calx buffer
 TODO, add vec overflow safey, if input larget then destination
 TODO  add destination parameter options?.. not yet
 TODO DONE make flipflop aware"
 (let [buf_to_inject 
       (cond 
         (cl-utils/is_buffer? input)
           input
         (vector? input)
           (doall (lg_wrap (:context @(:liquid_opencl_env liquid)) (map float input) :float32-le)))
       buf_to_inject_into (if @(:flipper liquid) :liquidState1_a_buf :liquidState1_b_buf )
       ]
   (lg_enqueue-kernel ((:liquid_queue liquid) @(:liquid_opencl_env liquid)) (:progs @(:liquid_opencl_env liquid))
                            :copyFloatXtoY
                            (cl-utils/buf_elements buf_to_inject) buf_to_inject (buf_to_inject_into liquid))
   (lg_enqueue-marker (@(:liquid_opencl_env liquid)(:liquid_queue liquid)))
))



(readoff_speced [liquid spec] "read the liquid state as specified by the spec, spec is a vec of start index, end index, eg: [0 12]"
  ;;TODO make :pp_answer_buf the default read out
  (let [flipbit @(:flipper liquid)]
    (if flipbit
        @(lg_enqueue-read (:liquidState1_b_buf liquid) ((:liquid_queue liquid) @(:liquid_opencl_env liquid)) spec)
        @(lg_enqueue-read (:liquidState1_a_buf liquid) ((:liquid_queue liquid) @(:liquid_opencl_env liquid)) spec)
        )))

(readoff [liquid] "read the full liquid state"
  ;;TODO make :pp_answer_buf the default read out
  (let [flipbit @(:flipper liquid)]
    (if flipbit 
        @(lg_enqueue-read (:liquidState1_b_buf liquid) ((:liquid_queue liquid) @(:liquid_opencl_env liquid)))
        @(lg_enqueue-read (:liquidState1_a_buf liquid) ((:liquid_queue liquid) @(:liquid_opencl_env liquid)))
        )))
)





(defn make_liquid 
"Returns a liquid state machine"
  [options]
   (let [{:keys [liquid_opencl_env liquid_queue liquidsize eta connections] 
                 :or 
                {liquid_opencl_env cl-utils/opencl_env
                 liquid_queue :queue   ;;TODO  Make this similart to pp, add qeueu to opencl_env dynamically ... or just make everything run on the default queue
                 liquidsize 64
                 eta (float 0.001)
                 connections 5
                   ;The default queue in the opencl_env
                 }
                } options
          liquidState1_a_buf  (doall (lg_wrap (:context @liquid_opencl_env) (utils/random_liquid_seedZ liquidsize 0.0) :float32-le))
          liquidState1_b_buf  (doall (lg_wrap (:context @liquid_opencl_env) (utils/random_liquid_seedZ liquidsize 0.0) :float32-le))   ;;   (doall (lg_create-buffer (:context @liquid_opencl_env) liquidsize :float32-le))
          liquidState2_a_buf  (doall (lg_wrap (:context @liquid_opencl_env) (utils/random_liquid_seedZ liquidsize 1.0) :float32-le))
          liquidState2_b_buf  (doall (lg_wrap (:context @liquid_opencl_env) (utils/random_liquid_seedZ liquidsize 1.0) :float32-le))   ;;(doall (lg_create-buffer (:context @liquid_opencl_env) liquidsize :float32-le))
          conectivity_buf     (doall (lg_wrap (:context @liquid_opencl_env) (utils/random_conectivity_seedZ liquidsize) :int32-le))
          debug_infobuff_buf  (doall (lg_create-buffer (:context @liquid_opencl_env) liquidsize :int32-le))
         ]
   (println liquidsize eta liquid_queue)
   
   (LiquidRecord. 
                         liquid_opencl_env
                         liquid_queue
                         (atom true)   ;flipper
                         
                         liquidState1_a_buf
                         liquidState1_b_buf
                         liquidState2_a_buf
                         liquidState2_b_buf
                         conectivity_buf
                         debug_infobuff_buf
                         connections
                         liquidsize)))




;;NOW WRITE TESTS!!!!


;(doall (lg_wrap (:context @opencl_env) (make_random_float_array size_of_alpha_needed -0.5 1) :float32-le))
;(def conectivity     (lg_wrap (:context @cl-utils/opencl_env) (random_conectivity_seedZ 14) :int32-le))

(quote
  
(def myliquid (make_liquid {:liquidsize (* 10) :connections 13}))
@(lg_enqueue-read (:liquidState1_b_buf myliquid) ((:liquid_queue myliquid) @(:liquid_opencl_env myliquid)))

@(lg_enqueue-read (:liquidState1_a_buf myliquid) ((:liquid_queue myliquid) @(:liquid_opencl_env myliquid)))
(inject myliquid  [45 34 23])
@(lg_enqueue-read (:liquidState1_a_buf myliquid) ((:liquid_queue myliquid) @(:liquid_opencl_env myliquid)))

(readoff myliquid)
(flop myliquid)
)

;;Note
;;buffers 'a' are from t-1. buffers 'b' are t bufferrs (those that need to be computed now)


;;   (time (run_liquid! globalsize connections sourceOpenCL)) (show_diagnostics)

(quote 

(def sourceOpenCL
  "
__kernel void flopliquid(
    __global int *conectivity,
    __global float *liquidState1_a,     
    __global float *liquidState1_b,
    __global float *liquidState2_a,     
    __global float *liquidState2_b,
    __global int *debug_infobuff,
    const int connections
        )   
{
    int gid = get_global_id(0);
    int gsize = get_global_size(0);

    int iatom = 0;                               //This is used to loop over all synapses in the neuron is connected to
    int random_value = 0;                        //This will be random value that will be used to procedurally create the synapse characterystics
    int inhibition_chooser = 0;                  //This will use the random value to also decide if this connection in inhibitory or exitory, note that coupling with position may lead to a non symetric system.
    int gid_to_read = 0;                         //This will the neuron to read from for this synapse, needs work to make it local.
    float liquidState1 = liquidState1_a[gid] ;   //Read in the Action Potential (aka: spike strength) of the current neuron at t-1.
    float liquidState2 = liquidState2_a[gid] ;   //Read in the Activation Potential (aka: least action potential needed to start a spike) from the t-1 buffer.
    float liquidState1_orig = liquidState1;      //Taking stock of the Action Potential before we start changing it       (looking to optimise away unnecessary writes later if we have no change.)
    float liquidState2_orig = liquidState2;      //Taking stock of the Activation Potential before we start changing it (looking to optimise away unnecessary writes)
    //__local float mylocalBuffer[1024];         //Future optimisation: could create a local buffer so that the write are coelesed __local float localBuffer[1024];  as per here http://stackoverflow.com/questions/2541929/how-do-i-use-local-memory-in-opencl

    uint m_z = conectivity[gid];                 //Randomnumber generator step: random number initial seed, this is needed per each kernel execution so that random number created are all different, else each kernel would produce same set of randoms, also allows for conectivity mutations (future research area), clojure creates this random number deterministically so executions are reproducable.
    uint m_w = 1;                                //Randomnumber generator step: Note, can not be gid because random number generator needs non zero initial values



if (liquidState1 > liquidState2) {               //If we have a spike, then.... very little to do...

liquidState1_b[gid] = 10.0;                      //Action Potential getting set to the spike level
liquidState2_b[gid] = 100.0;                     //Activation Potential getting set to the spike level, making another spike very unlikely untill it decays off

}
else  {
    for(iatom = 0; iatom < connections; iatom+=1 ) {
        m_z = 36969 * (m_z & 65535) + (m_z >> 16);    //Randomnumber generator step: As per http://en.wikipedia.org/wiki/Random_number_generation
        m_w = 18000 * (m_w & 65535) + (m_w >> 16);    //Randomnumber generator step:
        random_value = ((m_z << 16) + m_w);           //Randomnumber generator step: The large 32bit random number generated by the process for this synapse.
        inhibition_chooser = random_value % 100;      //Randomnumber generator step: Random number to determine inhibitory vs exitory connection
        gid_to_read = random_value % gsize;           //Randomnumber generator step: which nuron to read from     TODO: add locality via xyz space here... Note this is the only place that needs to know the liquid conectivity topology, this can also be created procedurally.

        //Note: LSM literature states that the neuron is inhibitory or exitory, not the synapse, so this model is wrong, would instead have to load in a random +1,-1 int arrey with correct distribution, or use signage of an existing vlaue, eg: liquidState1, assuming signage can never change during normal functioning  (idealy create procedurally to be able to replicate exactly.)

//Major refactor Jan 2012

        if (inhibition_chooser >= 20) {
          liquidState1 = liquidState1 + (liquidState1_a[gid_to_read] * 0.29);    //action potential getting subtracted or added based on Action Potential of connected neurons
          }
        else  {
          liquidState1 = liquidState1 - (liquidState1_a[gid_to_read] * 0.15);    //action potential getting subtracted or added based on Action Potential of connected neurons
          }
         }   // Closes the for loop over iatom , aka synapses

     // TODO: Speed optimisations possible here by preventing writes when values are not changing, (when just at the asimptotes).

     liquidState1_b[gid] = liquidState1 * 0.5;                                  // Writing out the Action Potential , which is reduced by the decay rate    TODO: could make this a parameter
     liquidState2_b[gid] = ((liquidState2 - 1.0) * 0.6 ) + 1.0;                 // Writing out the Activation Potential, Decaying it by 0.6, asymptoting it to 1.0.   TODO: long term other slower neuron dynamics can be added here by making the decay rate and assimptote represent neuronal adaptations to its typical activation rates.
                                                                                //TODO: Maas made the decay rates different if the neuron was exitory vs inhibitory, that's for another refactor...

}  //Closing the non spike firing else

        // Any writing out of bounds of an array corrupts memory further afield!!!
        // Note: ANY writing to the same location twice causes a catastrophic result fail, aka race condition

debug_infobuff[gid]= random_value;

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

;depricated: (def random_liquid_seed (doall (for [i (range (* globalsize))] (float (* 0.3 (rand))))))
;(defn random_liquid_seedZ [globalsizeZ] (doall (for [i (range (* globalsizeZ))] (float (* 0.3 (rand))))))
;(time(count (random_liquid_seedZ 10000000)))
;;Note, we want reproducable liquids, so lets make this a deterministic seed...

(defn random_liquid_seedZ [globalsizeZ booster]
   (doall
   (let [r (java.util.Random. 12345)]
     (loop [i 0 outlist ()]
       (if (= i globalsizeZ)
         outlist
         (recur (inc i) (conj outlist (+ (* (float (.nextInt r 100)) 0.1) booster) )))))))    ;Could probably do this via a reduce function?
;(round 0.0004  3)
;(random_liquid_seedZ 100 0)

(defn random_conectivity_seedZ [globalsizeZ]
   (doall
   (let [r (java.util.Random. 12345)]
     (loop [i 0 outarray []]
       (if (= i globalsizeZ)
         outarray
         (recur (inc i) (conj outarray (.nextInt r 64000) )))))))

;;;TODO lets make this a lazy sequence so that we can just stream these in on liquid bootstap and not take ram??

;(def foo (random_conectivity_seedZ (* 64)))
;(count foo)
;(time (count (random_conectivity_seedZ (* 100000000))))
;(time (count (random_conectivity_seedZ (* 100))))

(def init_liquid_status (atom true))
(defn init_liquid! [globalsizeZ]
  "Initialises the liquid states"
    ;(def conectivity     (create-buffer globalsize :int32))    ;A random used to influence the random number seed, so that a nuron can be rerolled if deemed poorly connected 
    (def conectivity     (wrap (random_conectivity_seedZ globalsizeZ) :int32))     ;A random used to influence the random number seed, so that a nuron can be rerolled if deemed poorly connected
    (def debug_infobuff  (create-buffer globalsizeZ :int32))    ;Vector used for debug information to emit out of the liquid
    ;    (def liquidState1_a  (create-buffer globalsize :float32))  ;Spike activation buffer A of the state double buffer
    (def liquidState1_a  (wrap (random_liquid_seedZ globalsizeZ 0.0)  :float32))
    (def liquidState1_b  (create-buffer globalsizeZ :float32))  ;Spike activation buffer B of the state double buffer
    (def liquidState2_a  (wrap (random_liquid_seedZ globalsizeZ 1.0) :float32)) ;Activation potential buffer A of the state double buffer
    (def liquidState2_b  (create-buffer globalsizeZ :float32))  ;Activation potential buffer B of the state double buffer
    (enqueue-barrier)(finish)
    ;(swap! init_liquid_status (fn [_] false))
    )


(def flop_liquid_status (atom true))
(defn flop_liquid! [globalsize connections liquidState1_A liquidState1_B liquidState2_A liquidState2_B debug_infobuff]
    (enqueue-kernel :flopliquid globalsize conectivity liquidState1_A liquidState1_B liquidState2_A liquidState2_B debug_infobuff connections)
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


;(defn floatround [s n] 
;     (.setScale (bigdec n) s java.math.RoundingMode/HALF_EVEN))

(def readout_liquid_status (atom true))

(defn readout_liquid! [whichliquidbuffer]
    (let [liquid_data (^floats float-array (deref (enqueue-read whichliquidbuffer [0 4096])))]
     (enqueue-barrier)(finish)
     (println 
       
            ;         (map  (fn [x] 
            ;           ;(floatround 1 (nth liquid_data x))
            ;        ;  (if (>= (nth liquid_data x) 9.9) (nth liquid_data x) (nth liquid_data x))
            ;           (nth liquid_data x)
            ;          )(range 0 64))
                     
                     
                     (map  (fn [x] 
                       ;(floatround 1 (nth liquid_data x))
                    ;  (if (>= (nth liquid_data x) 9.9) (nth liquid_data x) (nth liquid_data x))
                       (if (>= (nth liquid_data x) 9.9) x 0)
                      )(range 0 64))
              "total on:" (reduce + 0 (map (fn [x] 
                      ;(floatround 0 (nth liquid_data x))
                      (if (>= (nth liquid_data x) 9.9) 1 0)
                      )(range 0 4096))   )
    )
     liquid_data
     )
    ;(swap! readout_liquid_status (fn [_] false))
    ;(swap! readout_liquid_status (fn [_] true))
)

;    (time (run_liquid! globalsize connections sourceOpenCL)) (show_diagnostics)


(def readout_liquid_ints_status (atom true))
(defn readout_liquid_ints! [whichliquidbuffer]
    (let [liquid_data (^ints int-array (deref (enqueue-read whichliquidbuffer [0 4096])))]
     (enqueue-barrier)(finish)
     (println 
                     (map  (fn [x] 
                      (nth liquid_data x)
                      )(range 0 64))
    ))
    (enqueue-barrier)(finish)
    ;(swap! readout_liquid_status (fn [_] false))
    ;(swap! readout_liquid_status (fn [_] true))
)


(defn run_liquid! [globalsizeZ connections OpenCLSourceToUse]
  (with-cl
     (if @init_liquid_status (init_liquid! globalsizeZ))  (enqueue-barrier)(finish)
     (def checkpoint1_start (. System (nanoTime)))
      ;Main with-cl loop starts here
     (with-program (compile-program OpenCLSourceToUse)
        
        (loop [k opencl_loops]
          ;; For some rason, only when size of array is above (* 64 64 64), the next lines is causing an InvalidCommandQueue error...
          (if @readout_liquid_status (readout_liquid! liquidState1_a))
          ;(if @readout_liquid_ints_status (readout_liquid_ints! debug_infobuff))
          (if @flop_liquid_status (flop_liquid! globalsizeZ connections liquidState1_a liquidState1_b liquidState2_a liquidState2_b debug_infobuff))

   ;comment this back in for more diagnostics   ;
          ;(if @readout_liquid_status (readout_liquid! liquidState1_b))
          ;;(if @readout_liquid_status (readout_liquid! liquidState2_a))
          ;(if @readout_liquid_ints_status (readout_liquid_ints! debug_infobuff))
          (if @flop_liquid_status (flop_liquid! globalsizeZ connections liquidState1_b liquidState1_a liquidState2_b liquidState2_a debug_infobuff))
          (if @readout_liquid_status (readout_liquid! liquidState1_b))
          
        (Thread/sleep 0)
        (if (= k 1) nil (recur (dec k))))
        )
      (def checkpoint1_end (. System (nanoTime)))
      "Terminating liquid"
      ;Main with-cl loop ends here
    ;(println "to infinity, and beyond" k)
   ))

(defn show_diagnostics []
(println "Liquid Flops per second  : "(/ (* opencl_loops 2) (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0)))
(println "Billion synapse evaluation per second: "(/ (/ (* globalsize connections opencl_loops 2) (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0)) 1000000000))
(println "Number of synapses: "(* globalsize connections)   "  Number of neurons:" globalsize )
)

(def globalsize (* 64 64 64))  ;Total size of liquid
(def connections 500)                 ;Number of neurons each neuron should connect to, a double connection is more and more likely with the size...
(def opencl_loops 100)


;   (swap! readout_liquid_status (fn [_] false))
;   (swap! readout_liquid_status (fn [_] true))
;       (time (run_liquid! globalsize connections sourceOpenCL)) (show_diagnostics)


;TODO, auto benchmark over a space of options, 2 dimentions, intervals over which to test, increments , measure to use)... injecting into with-cl as needed... 

;Accurate Time (/ (/ (- checkpoint1_end checkpoint1_start ) 1000000000.0) opencl_loops)
;;Billion operations per second...
 ;     (total operations                       ) (total seconds                                        )  billion   )

;end on quote
)

(println "All Looks OK code wise... in liquid.clj")

;run this
;;TODO, be able to 'run' things within a threaded with-cl (that's in a loop), by running a function....
;;TODO change the definiton of that runs within the winth-cl, while it's running by redefining the call being made.... via macros or atoms
;;TODO DONE: Have a loop that loops over each statement within with-cl, quickly checking if it should be executed or not, if yes, executing, seting state back to 'done'


