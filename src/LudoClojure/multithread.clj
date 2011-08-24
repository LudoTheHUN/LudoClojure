


;http://stackoverflow.com/questions/4768448/synchronising-threads-for-multiple-readers-single-writer-in-clojure


;;This shows how a single growing 'ref array' can be touched by many threads...
;;performance is not stellar with some time before million updates happen, but that is with contention...


;;TODO what happens with maps? will they have a contention on a per item or global object level?
;;TODO how to measure contention?
;;TODO what setup will minimise contention in a nuronal +self referencing setup?

;;TODO how many simulatanious threads can we have? can we have visibility into howmany are running?


(defn reader [shared] 
  ;(println "I see" @shared)
   (count @shared))

(defn writer [shared item]
  (dosync 
   ;; (println "Writing to shared")
    (alter shared conj item)))

;; combine the read and the write in a transaction
(defn combine [shared item]
  (dosync 
    (reader shared)
    (writer shared item)))

;; run a loop that adds n thread-specific items to the ref
(defn test-loop [shared n]
  (doseq [i (range n)]
    (combine shared (str (System/identityHashCode (Thread/currentThread))))
   ; (combine shared (str (System/identityHashCode (Thread/currentThread)) "-" i))
    ;(Thread/sleep 5)
))

;; run t threads adding n items in parallel
(defn test-threaded [t n]
  (let [shared (ref [])]
    (doseq [_ (range t)]
      (future (test-loop shared n)))))

(defn test-threaded [t n]
  (let [shared (ref [])]
    (doseq [_ (range t)]
      (future (test-loop shared n)))
    shared  ;; this makes the 'shared' ref the output of this call... but note that it's status will be final after the delay ....
   ))

(test-threaded 3 10)

(def outputvalue (test-threaded 300 10000))
;(def outputvalue 0)
(count @outputvalue)
(count (distinct @outputvalue))
;; Note how outputvalue slowly fills up as the threads complete...
(def shared (ref []))







