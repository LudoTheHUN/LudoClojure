(ns LudoClojure.mocs
  ;(:require calx)
  )

(use 'calx)

;Here I'll try to mock out high level functions for liquids, paralel perceptrons and thier composig...



;(def my_pp (make-pp aconfig))   ; makes a paralel perceptron called my_pp

;((my_pp :init_pp!))


(def a (atom 1000000))
(time (while (pos? @a) (do (swap! a dec))))



;;Mocking out a remote queue out system...

;TODO   strucutued queue of id:functions, must be compatible with rifle pperceptrons.
(defn make_new_openCL_queue [] (ref {:jobid 0 :response {} :queue clojure.lang.PersistentQueue/EMPTY }))

(def aPersistentQueue (make_new_openCL_queue))


;;;http://www.ibm.com/developerworks/java/library/wa-clojure/index.html
(defn add_to_queue [queue_name functiontoenque]
  "this gets called outside openCL thread, but passes in commands to be done within openCL context"
   ;TODO must return job ID under which job is being submited, so that return can be read of later from results map
   (let [local_jobid (ref :notset)]
   (dosync    (ref-set local_jobid (:jobid @queue_name))
              (ref-set queue_name (assoc @queue_name 
                                  :jobid (inc (:jobid @queue_name))
                                  :response (conj (:response @queue_name) [(:jobid @queue_name) :awaiting_response])
                                  :queue (conj (:queue @queue_name) [(:jobid @queue_name) functiontoenque])))
           ;;Also add a place holder responce onto the response atom
     )
     @local_jobid))


; Something like this passes in openCL calls, outside of the openCL scope
(add_to_queue aPersistentQueue (fn [] "foo"))
(add_to_queue aPersistentQueue (fn [] "boo"))
(add_to_queue aPersistentQueue (fn [] (+ 1 2)))
(add_to_queue aPersistentQueue (fn [] (println 4)))
(add_to_queue aPersistentQueue (fn [] (println (+ 1 2))))

;(defn perma_pop [queue_name]
;     (swap! queue_name (fn [x] (assoc x :queue (pop (:queue x)))))
;     (peek (@queue_name :queue))
;     )
;(perma_pop aPersistentQueue)

(defn make_a_response [itemarray]
  "second items is expected to be a function, it will be executed here"
    [(first itemarray) ((second itemarray))])

(defn do_and_pop_queue_item [queue_name]
  "as super safe, I hope, queue and runner, this runs in openCL context"
    (let [local_queue (ref :notset)]
            (dosync 
                             (ref-set local_queue (@queue_name :queue))
                             (ref-set queue_name (assoc @queue_name :queue (pop (:queue @queue_name)))))
      ;;TODO make a catch here that will stop an empty queue from doing much....eg not erroring...
      (if (peek @local_queue)
          (make_a_response (peek @local_queue))
          nil
               )))



;(defn gather_responses_old [response_mapatom response]
;    "this holds results of computations done on openCL"
;       (swap! response_mapatom  (fn [x] (conj x response))))

(defn gather_responses [queue_name response]
    "this holds results of computations done on openCL"
       (if response
       (dosync
           (ref-set queue_name (assoc @queue_name 
                                   :response (conj (:response @queue_name) [(first response) (second response)]))))))


;;;; This will not write to responses, so never do it on its onwn:
;(do_and_pop_queue_item aPersistentQueue)

"something like this runs under openCL" 
(gather_responses aPersistentQueue (do_and_pop_queue_item aPersistentQueue))




(defn read_response [jobid queue_name]
    (let [response ((:response @queue_name) jobid)]
      (if (= response :awaiting_response)
        :awaiting_response
         (dosync
           (ref-set queue_name (assoc @queue_name 
                                   :response (dissoc (:response @queue_name) jobid)))))
      response))

;You can read response only once... unless result is :awaiting_response, in which case, keep trying to get a value out...
(read_response 15 aPersistentQueue)



;Putting it all together....
(defn start_pp_openCL_on_thread! [queue_name pp_openCL]
  ;;TODO Add a check to ensure the queue is not already running
  ;;TODO Add a decomission mechanisim

  (.start (Thread. (fn []
     (with-cl (with-program (compile-program pp_openCL)
  
;(with-cl (with-program (compile-program pp_openCL)
 ;;Put on loop
   (loop [k 2000000]
     ;(println "on step:" k)
     (gather_responses queue_name (do_and_pop_queue_item queue_name))
     
     ;TODO have a status i nthe quenue object!
     (Thread/sleep 1)
  (if (= k 1) 1 (recur (dec k) )))
;))
))))))
;TODO!! prevent the same queue_name executor from being run(initiated) twice with queue runnig state

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


(start_pp_openCL_on_thread! aPersistentQueue pp_openCL)



(add_to_queue aPersistentQueue (fn [] "boo"))
(add_to_queue aPersistentQueue (fn [] (+ 1 2)))
(add_to_queue aPersistentQueue (fn [] (println 45)))

(add_to_queue aPersistentQueue (fn [] (do (println (+ 1 2)) "fooo")))


       ;CONTINUE HERE
       
(defn do_via_queue! [queue_name fun]
 (let [response_is_on_id (add_to_queue queue_name fun)]   ; assuming the queue is running
       ;!!! can use while the test will destroy the value we want to read off!, (while 
   (loop [k 1000]
      (let [response (read_response response_is_on_id queue_name)]
           (if (or (not (= response :awaiting_response)) (= k 0))
               (if 
                   (= k 0)
                   :response_timeout
                   response)
               (do (Thread/sleep 1) (recur (dec k))))))))
           
     ;(println "on step:" k)
 
     
     ;TODO have a status in the quenue object!

            
 ;          (let resonse [(read_response response_is_on_id queue_name)]
  ;       (= (read_response response_is_on_id queue_name) :awaiting_response))
   ;    (read_response response_is_on_id queue_name)))

(do_via_queue! aPersistentQueue (fn [] (do (Thread/sleep 25) (println (+ 69 2)) "fooo")))
(keys (@aPersistentQueue :response))
(do_via_queue! aPersistentQueue (fn [] 64))
               

@aPersistentQueue



(println "quick openCL test, works a charm")

;(def conectivity     (wrap ([12 13 5 45] 3) :int32))
(do_via_queue! aPersistentQueue (fn [] (def openclarray     (wrap [(float 12.0) (float 12.0) (float 12.0) (float 12.0)] :float32))))
(do_via_queue! aPersistentQueue (fn [] (def openclarray2     (wrap [(float 12.0) (float 12.0) (float 12.0) (float 12.0)] :float32))))

(do_via_queue! aPersistentQueue (fn [] (enqueue-kernel :foopp 4 openclarray openclarray2  )))
(do_via_queue! aPersistentQueue (fn [] (enqueue-kernel :foopp 4 openclarray2 openclarray  )))

(defn readout_float_buffer [whichbuffer start_read_at end_read_at]
    (let [buffer_data (^floats float-array (deref (enqueue-read whichbuffer [start_read_at end_read_at])))]
     (enqueue-barrier)(finish)
     (let [clj_arrayout (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at)))]
     (println clj_arrayout)
     clj_arrayout
     ))
)

(do_via_queue! aPersistentQueue (fn [] (readout_float_buffer openclarray 0 4)))
(do_via_queue! aPersistentQueue (fn [] (readout_float_buffer openclarray2 0 4)))




;Results get gathered up in a response map like this:

;(defn read_response_mapatom_OLD [jobid response_mapatom]
;    (let [response (@response_mapatom jobid)]
;      (swap! response_mapatom  (fn [x] (dissoc x jobid)))
;      response))
;
;(read_response_mapatom 2 response_mapatom)

;Reading off a response needs to purge the map of the result, to keep size down...

;TODO 

; (swap! queue_name (fn [x] (assoc x :queue (pop (:queue x)))))
;((second (peek (@aPersistentQueue :queue))))

(peek (@aPersistentQueue :queue))
(peek (pop (@aPersistentQueue :queue)))
(peek (pop (pop (@aPersistentQueue :queue))))



