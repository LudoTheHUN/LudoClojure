(ns LudoClojure.mocs)



;Here I'll try to mock out high level functions for liquids, paralel perceptrons and thier composig...



;(def my_pp (make-pp aconfig))   ; makes a paralel perceptron called my_pp

;((my_pp :init_pp!))


(def a (atom 1000000))
(time (while (pos? @a) (do (swap! a dec))))



;;Mocking out a remote queue out system...

;TODO   strucutued queue of id:functions, must be compatible with rifle pperceptrons.

(def aPersistentQueue (ref {:jobid 0 :queue clojure.lang.PersistentQueue/EMPTY }))


;;;http://www.ibm.com/developerworks/java/library/wa-clojure/index.html
(defn add_to_queue [queue_name functiontoenque]
  "this gets called outside openCL thread, but passes in commands to be done within openCL context"
   ;TODO must return job ID under which job is being submited, so that return can be read of later from results map
   (dosync (ref-set queue_name (assoc @queue_name 
                                  :jobid (inc (:jobid @queue_name)) 
                                  :queue (conj (:queue @queue_name) [(:jobid @queue_name) functiontoenque])))))

(add_to_queue aPersistentQueue (fn [] "foo"))
(add_to_queue aPersistentQueue (fn [] "boo"))

;(defn perma_pop [queue_name]
;     (swap! queue_name (fn [x] (assoc x :queue (pop (:queue x)))))
;     (peek (@queue_name :queue))
;     )
;(perma_pop aPersistentQueue)

(defn make_a_response [itemarray]
  "second items is expected to be a function, it will be executed here"
    {(first itemarray) ((second itemarray))})

(defn do_and_pop_queue_item [queue_name]
  "as super safe, I hope, queue and runner, this runs in openCL context"
    (let [local_queue (ref 67)]
            (dosync 
                             (ref-set local_queue @queue_name)
                             (ref-set queue_name (assoc @queue_name :queue (pop (:queue @queue_name)))))
      ;;TODO make a catch here that will stop an empty queue from doing much....
      (make_a_response (peek (@local_queue :queue)))))

(def response_mapatom (atom {}))

(defn gather_responses [response_mapatom response]
    "this holds results of computations done on openCL"
       (swap! response_mapatom  (fn [x] (conj x response))))


(do_and_pop_queue_item aPersistentQueue)

"something like this runs under openCL" 
(gather_responses response_mapatom (do_and_pop_queue_item aPersistentQueue))


response_mapatom

;TODO 




   ; (swap! queue_name (fn [x] (assoc x :queue (pop (:queue x)))))


   
;((second (peek (@aPersistentQueue :queue))))


(peek (@aPersistentQueue :queue))


(peek (pop (@aPersistentQueue :queue)))
(peek (pop (pop (@aPersistentQueue :queue))))



