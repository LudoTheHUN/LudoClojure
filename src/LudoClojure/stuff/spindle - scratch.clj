

;;;;;;;;  END OF SPINDLE CODE;;;;;;;;  END OF SPINDLE CODE
;;;;;;;;  END OF SPINDLE CODE;;;;;;;;  END OF SPINDLE CODE
;;;;;;;;  END OF SPINDLE CODE;;;;;;;;  END OF SPINDLE CODE

(comment
  
(defn NOW_REDUNDANTstart_openCL_spindle! [spindle openCL_source]
  "Sets the spindle spinning status to true and start to read off the queue,
   calling spin! on it. " 
  ;;TODO Add a check to ensure the queue is not already running
  ;;TODO Add a decomission mechanisim
  ;TODO write tests  !!!!!!
  ;TODO write tests
  ;TODO write tests
 (let [ok_to_start_spindle (ref :donno)]
    (dosync
           (if (= (@spindle :spinning?) false)
               (do (ref-set spindle (assoc @spindle :spinning? true))
                   (ref-set ok_to_start_spindle :yes))
               (ref-set ok_to_start_spindle :no)
                  ))
   (if (and (= @ok_to_start_spindle :yes) @spindle)
       (.start (Thread. (fn []
         (with-cl (with-program (compile-program openCL_source)
           (while (= (@spindle :spinning?) true)
             (do 
                (try (spin! spindle)
                   (catch Exception e 
                     (do 
                     (println "ERROR - spin! failed, stopping spindle")
                     (stop_spindle! spindle)
                     )))
                (if (= (peek (@spindle :queue)) nil) (Thread/sleep 1))))))
         :spindle_stopped)))
   :not_ok_to_start_spindle)))
  
  
(def  foo_spindle (make_spindle))
(weave_on! foo_spindle #(+ 3 6))

(spin_once! foo_spindle)
((@foo_spindle :response) 0)
(swap! ((@foo_spindle :response) 0) (fn [_](+ 42)))
(spool_on! foo_spindle [0 798])

(spool_on! foo_spindle (spin_once! foo_spindle))

(weave_on! foo_spindle #(+ 10))
(spin! foo_spindle)
(spool_off! foo_spindle 2)

(weave_off! foo_spindle 6)
(weave_off! foo_spindle 3)

(start_spindle! foo_spindle)
(stop_spindle! foo_spindle)

(time (weave! foo_spindle #(+ 10)))
(peek (@foo_spindle :queue))

(time (loop [k 100]
     (if  (= k 0)
        :done
        (do (weave! foo_spindle #(+ 10))
            (recur (dec k))))))

(spool_off_dump! foo_spindle)



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
(start_openCL_spindle! foo_spindle pp_openCL)

(time (do
(weave! foo_spindle (fn [] (def openclarray (wrap [(float 12.0) (float 12.0) (float 12.0) (float 12.0)] :float32))))
(weave! foo_spindle (fn [] (def openclarray2     (wrap [(float 15.0) (float 16.0) (float 170.0) (float 18.0)] :float32))))
(weave! foo_spindle (fn [] (enqueue-kernel :foopp 4 openclarray2 openclarray  )))
(weave! foo_spindle (fn [] (enqueue-kernel :foopp 4 openclarray openclarray2  )))
(defn readout_float_buffer [whichbuffer start_read_at end_read_at]
    (let [buffer_data (^floats float-array (deref (enqueue-read whichbuffer [start_read_at end_read_at])))]
     (enqueue-barrier)(finish)
     (let [clj_arrayout (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at)))]
     clj_arrayout)))
(weave! foo_spindle (fn [] (readout_float_buffer openclarray 0 4)))
))

(time (spool_off_dump! foo_spindle))
(time (spin_dump! foo_spindle))
(stop_spindle! foo_spindle)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;s
(def  foo_spindle3 (make_spindle))

(start_openCL_spindle! foo_spindle3 pp_openCL)

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

(defn readout_float_buffer [whichbuffer start_read_at end_read_at]
    (let [buffer_data (^floats float-array (deref (enqueue-read whichbuffer [start_read_at end_read_at])))]
     (enqueue-barrier)(finish)
     (let [clj_arrayout (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at)))]
     clj_arrayout)))
(weave_away! foo_spindle3 (fn [] (def openclarray (wrap [(float 12.0) (float 12.0) (float 12.0) (float 12.0)] :float32))))
(weave_away! foo_spindle3 (fn [] (def openclarray2     (wrap [(float 15.0) (float 16.0) (float 170.0) (float 18.0)] :float32))))

(time (loop [k 10000]
     (if  (= k 0)
        :done
        (do 

;(start_openCL_spindle! foo_spindle3 pp_openCL)

           (do
;(weave_on! foo_spindle3 (fn [] (def openclarray (wrap [(float 12.0) (float 12.0) (float 12.0) (float 12.0)] :float32))))
;(weave_on! foo_spindle3 (fn [] (def openclarray2     (wrap [(float 15.0) (float 16.0) (float 170.0) (float 18.0)] :float32))))
(weave_away! foo_spindle3 (fn [] (enqueue-kernel :foopp 4 openclarray2 openclarray  )))
(weave_away! foo_spindle3 (fn [] (enqueue-kernel :foopp 4 openclarray openclarray2  )))
(defn readout_float_buffer [whichbuffer start_read_at end_read_at]
    (let [buffer_data (^floats float-array (deref (enqueue-read whichbuffer [start_read_at end_read_at])))]
     (enqueue-barrier)(finish)
     (let [clj_arrayout (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at)))]
     clj_arrayout)))
;(println (weave! foo_spindle3 (fn [] (readout_float_buffer openclarray 0 4))))
)
;(time (spool_off_dump! foo_spindle3))
;(time (spin_dump! foo_spindle3))
;(stop_spindle! foo_spindle3)

            (recur (dec k))))))
(println (weave! foo_spindle3 (fn [] (readout_float_buffer openclarray 0 4))))
)


      ;;;Old approach was changing the ref.... but the ref already holds the atom which we want to deliver the answer to
         ;;so just local and deliver the answer....
       ;(dosync
       ;    (ref-set spindle (assoc @spindle 
       ;                            :response (conj (:response @spindle) done_job))))))

;


















      

;;;;;;;;;;;;;;;;OLD CODE BELOW THIS LINE ONLY;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;Here I'll try to mock out high level functions for liquids, paralel perceptrons and thier composig...

;TODO the queue entry needs to hold the thread object of the caller, so that the calling thread can be interupted.
;At startup, the tread object of the executor should be put on the queue object so that the calling thread can wake up the executor when there is work to do



;(def my_pp (make-pp aconfig))   ; makes a paralel perceptron called my_pp

;((my_pp :init_pp!))

(comment 
  
  
(def a (atom 10000000))
(time (while (pos? @a) (do (swap! a dec))))

(pos? 1)

;;Mocking out a remote queue out system...

;TODO   strucutued queue of id:functions, must be compatible with rifle pperceptrons.
(defn make_new_openCL_queue [] (ref {:jobid 0 :response {} :queue clojure.lang.PersistentQueue/EMPTY }))
;TODO break this out into an object storing the response in a seperate ref...or maybe just atom?

(def aPersistentQueue (make_new_openCL_queue))

@@(ref (atom :foo))


;;;http://www.ibm.com/developerworks/java/library/wa-clojure/index.html
(defn add_to_queue [queue_name functiontoenque]
  "this gets called outside openCL thread, but passes in commands to be done within openCL context"
   ;TODO must return job ID under which job is being submited, so that return can be read of later from results map
   (let [local_jobid (ref :notset)]
   (dosync    (ref-set local_jobid (:jobid @queue_name))
              (ref-set queue_name (assoc @queue_name 
                                  :jobid (inc (:jobid @queue_name))
                                  :response (conj (:response @queue_name) [(:jobid @queue_name) :awaiting_response])  ;;TODO make this await_responce here sit in an atom...
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
  "as super safe, I hope, queue and runner, this is intended to run in openCL context"
    (let [local_queue (ref :notset)]
            (dosync 
                             (ref-set local_queue (@queue_name :queue))
                             (ref-set queue_name (assoc @queue_name :queue (pop (:queue @queue_name)))))
      (if (peek @local_queue)
          (make_a_response (peek @local_queue))
          nil)))



;(defn gather_responses_old [response_mapatom response]
;    "this holds results of computations done on openCL"
;       (swap! response_mapatom  (fn [x] (conj x response))))

(defn gather_responses [queue_name response]
    "this gathers up results of computations done on openCL onto a map of jobid to response values"
       (if response
       (dosync
           (ref-set queue_name (assoc @queue_name 
                                   ;:response (conj (:response @queue_name) [(first response) (second response)]))))))
                                   :response (conj (:response @queue_name) response))))))  ;note, response is already a 2 item array

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


;;This is an example use case...
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
     ;just have this loop for ever while running
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

       ;;name to be changed to weave_off!
(defn do_via_queue! [queue_name fun]
 (let [response_is_on_id (add_to_queue queue_name fun)]   ; assuming the queue is running
       ;!!! can use while the test will destroy the value we want to read off!, (while 
   (loop [k 10000]     ;;TODO replace this with an interupt on the function's calling thread object
      (let [response (read_response response_is_on_id queue_name)]
           (if (or (not (= response :awaiting_response)) (= k 0))
               (if 
                   (= k 0)
                   :response_timeout
                   response)
               (do (Thread/sleep 1) (recur (dec k))))))))    ;looping like this seems bad, java.util.concurrent is supposed to have features fo this...
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
(time (do_via_queue! aPersistentQueue (fn [] (enqueue-kernel :foopp 4 openclarray2 openclarray  ))))

(time (let [foo 4] 
    (do_via_queue! aPersistentQueue (fn [] (enqueue-kernel :foopp foo openclarray2 openclarray  )))))
(time (let [foo 4] 
    (do_via_queue! aPersistentQueue (fn [] (enqueue-kernel :foopp foo openclarray openclarray2  )))))
;lexical closure still works

(defn readout_float_buffer [whichbuffer start_read_at end_read_at]
    (let [buffer_data (^floats float-array (deref (enqueue-read whichbuffer [start_read_at end_read_at])))]
     (enqueue-barrier)(finish)
     (let [clj_arrayout (map  (fn [x] (nth buffer_data x))(range 0 (- end_read_at start_read_at)))]
     (println clj_arrayout)
     clj_arrayout
     ))
)

(time (do_via_queue! aPersistentQueue (fn [] (readout_float_buffer openclarray 0 4))))
(do_via_queue! aPersistentQueue (fn [] (readout_float_buffer openclarray2 0 4)))

(do_via_queue! aPersistentQueue #(readout_float_buffer openclarray2 0 4))


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

;;;; This is mostly to show that intrupting threads is fast (enough).... 0.05ms per interupt.

(def foo (atom "threadopbject"))
(.start (Thread.
  (fn [] 
     (swap! foo (fn [x] (Thread/currentThread)))
      (println "before sleep")
      
     (time (try (Thread/sleep 10000) (catch InterruptedException e (println "rise and shine"))))
     
     (loop [k 100]
          (if (= k 0)
                   :got_to_the_end
                   (do (time (try (Thread/sleep 1000) (catch InterruptedException e (println "1ms sleep in loop woke up , and on k=" k)))) (recur (dec k)))
                   )
               )
      (println "and done"))))


;;Method being called on an object!!!
;;(.getId @foo)
(.interrupt @foo)

;;(.notify @foo)

(time (loop [k 300000]
          (if (= k 0)
                   :got_to_the_end
                   (do (.interrupt @foo) (recur (dec k)))
                   )))


(loop [k 300]
          (if (= k 0)
                   :got_to_the_end
                   (do (time (println "on k=" k)) (recur (dec k)))
                   )
               )



;;spawn insane number of threads
(loop [threadnumber 100]
     (if (= threadnumber 0)
      :started_all_threads
 (do
  (.start (Thread.
  (fn [] 
     (swap! foo (fn [x] (Thread/currentThread)))
      (if (= (mod threadnumber 10000) 1) (println "before sleep" threadnumber))
      
     (try (catch InterruptedException e (println "rise and shine")))
     
     (loop [k 10]
          (if (= k 0)
                   :got_to_the_end
                   (do (Thread/sleep 1000) 
                        (if (= (mod threadnumber 10000) 1)
                          (println "threadnumber:" threadnumber )) (recur (dec k)))
                   )
               )
      (println "and done"))))
  
  (recur (dec threadnumber)))))


)


