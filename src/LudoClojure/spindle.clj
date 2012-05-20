(ns ^{:doc "Spindle, reactor like construct for teleporting unevaluated 
      functions and their returns values between threads for use in siturations 
      where the scope of a thread is expensive to bootstap eg: within calx and 
      with opencl scopes."
      :author "LudoTheHUN"}
   LudoClojure.spindle)

(use 'calx)


(defn make_spindle 
^{:doc "Creates a spindle. Holds jobid that will be assigned to the next task, 
          a set of uncollected responses (return values of functions)
          and a queue of tasks that are waiting to be carries out. Also holds 
          some status information and options"}
   ([] (make_spindle 1000 1 :default))
   ([weave_off_retries weave_off_ms_wait] 
      (make_spindle weave_off_retries weave_off_ms_wait :default))
   ([weave_off_retries weave_off_ms_wait spindle_name] 
   (ref {:jobid 0 
         :response {}
         :queue clojure.lang.PersistentQueue/EMPTY
         :spinning? false
         :weave_off_retries weave_off_retries
         :weave_off_ms_wait weave_off_ms_wait
         :spindle_name spindle_name})))

;(def a_spindle (make_spindle))

(defn weave_on! [spindle fun]
^{:doc "Adds a function to be executed onto the spindle specified. You can 
          call this function on any thread. Returns the jobid linking this call 
          with the eventual answer. Adds an entry onto response: awaiting answer
          Should return very quickly"}
 (let [local_jobid (ref :notset)]
    (dosync (ref-set local_jobid (:jobid @spindle))
            (ref-set spindle 
               (assoc @spindle 
                  :jobid 
                     (inc (:jobid @spindle))
                  :response 
                     (conj (:response @spindle) 
                           [(:jobid @spindle) (atom :awaiting_response)])
                  :queue 
                     (conj (:queue @spindle) 
                            [(:jobid @spindle) fun true]))))
  @local_jobid))

(defn weave_away! [spindle fun]
^{:doc "For SIDE EFFECTS only!
          Adds a function to be executed onto the spindle specified. You can 
          call this function on any thread. Returns the jobid this work will be
          done on, however no answer will be retrivable from the responses map
          Should return very quickly. Side effects will be performed by the
          spinning thread, or jobid order (only if there is just one spining
          thread)"}
;;TODO TESTs
 (let [local_jobid (ref :notset)]
    (dosync (ref-set local_jobid (:jobid @spindle))
            (ref-set spindle 
               (assoc @spindle 
                  :jobid 
                     (inc (:jobid @spindle))
                  :queue 
                     (conj (:queue @spindle) 
                            [(:jobid @spindle) fun false]))))
  @local_jobid))


(defn do_job- [job]
^{:doc "takes a job tuple and creates the done job tuple, executes the given
        function held in the job
        The array is  jobid, function to call, bool if result should be kept for 
        reading"}
    [(first job) 
     (try ((second job))
        (catch Exception e (do (println "ERROR inner spindle call failed" (second job) "The trace is: " e )
                             :error_in_spun_function)))
     (nth job 2)])
;;TODO put another try catch here around ((second job)) to prevent locking up sindles.
;;TODO write tests for bad fuctions being passed in... that show spindle is robust.


(defn spin_once! [spindle]
^{:doc "Takes a job off the spindle and executes it
        No retries are possible.
        The spinning should be done on the target thread which has the expensive
        openCL context."}
 (let [local_job (ref :notset)]
    (dosync 
       (ref-set local_job (peek (@spindle :queue)))
       (if (= nil @local_job)
         :nothing_to_spin
         (ref-set spindle (assoc @spindle :queue (pop (:queue @spindle))))))
    (if @local_job
       (do_job- @local_job)
       :nothing_to_spin)))

(defn spin_dump! [spindle]
^{:doc "dumps all data in the spindle queue"}
    (dosync
       (ref-set spindle (assoc 
                          @spindle :queue 
                          clojure.lang.PersistentQueue/EMPTY))))

(defn spool_on! [spindle done_job]
^{:doc "puts the response of the done function, the done_job, onto the spindle
        response map of jobid:atoms"}
;;;TODO a bug:NullPointerException is somehow possible somewhere here, 
      ;;look into it
   (if (= done_job :nothing_to_spin)
    :nothing_to_spin
     (if (nth done_job 2)
       (let [response_atom_to_update ((@spindle :response) (first done_job))]
         (swap! response_atom_to_update (fn [_] (second done_job))))
       nil)))

(defn spin! [spindle]
^{:doc "Spins the spindle. Gets the result of given function executed and
        spools on the result back on.
        This should be run o threads that have expensive to spool up contexts"}
  (let [spun_result (spin_once! spindle)]
    (if (= spun_result :nothing_to_spin)
        :nothing_to_spin
        (spool_on! spindle spun_result))))


(defn spool_off! [spindle jobid]
^{:doc "Returns the responce for a jobid that has been previouslty spooled on.
        if the response is found, removes the job response from spindle response 
        map.
        If response is still :awaiting_response, returns :awaiting_response.
        If there is no response, returns :response_missing.
        Does not guarantee that if two threads reading off a jobid from a 
        spindle at the very same time end up with exactly only one having the
        answer delived.
"}
;;TODO maybe, put everything in dosync so that one amswer is guaranteed to be 
;;retived once?? Bad for performance / locking characterystics
    (let [response 
                (try @((:response @spindle) jobid) 
                   (catch java.lang.NullPointerException e :response_missing))
                ]
      (if (= response :awaiting_response)
        :awaiting_response
         (dosync
           (ref-set spindle (assoc @spindle 
                               :response (dissoc (:response @spindle) jobid)))))
      response))

(defn spool_off_dump! [spindle]
^{:doc "clears out all uncollected responses"}
  (dosync 
    (let [ids_to_dump  (keys (:response @spindle))]
      (ref-set spindle (assoc @spindle  :response 
                          (reduce dissoc (:response @spindle) ids_to_dump))))))


(defn weave_off! [spindle jobid]
^{:doc "Returns with the return values of the function called on a jobid
        trys to spool_off! results from spindle untill retry attempts run out
        gives up if result is :response_missing"}
   (let  [weave_off_retries (@spindle :weave_off_retries)
          weave_off_ms_wait (@spindle :weave_off_ms_wait)]
    (loop [k weave_off_retries]
      (let [response (spool_off! spindle jobid )]
           (if (or (not (= response :awaiting_response)) 
                   (= response :response_missing)
                   (= k 0))
               (if 
                   (= k 0)
                   :response_timeout
                   response)
               (do (Thread/sleep weave_off_ms_wait) (recur (dec k))))))))


(defn weave! [spindle fun]
^{:doc "The main function for spindle, weaves the function on and weaves the
        result back , returning it. Assumes spindle is spinning.
        Returns the response of the provided function via the spindle round 
        trip"}
;;TODO write more tests!!
  (let [jobid (weave_on! spindle fun)]   ; assuming the queue is running
    (weave_off! spindle jobid)))




(defn stop_spindle! [spindle]
^{:doc "Stops a spinnig sindle by setting it's spinning? flag. Any correclty
        implemented spindle will complete it current jobid and exit out of the
        worker thread."}
;TODO write more tests
;TODO this is effectively broken, the spindle can continue for unknown amount of
;time
     (dosync (ref-set spindle (assoc @spindle :spinning? false)))
     :spindle_stopping)


(defn safe_spin! [spindle]
^{:doc "spins the spindle in a trycatch, catching all errors."}
;;TODO need to test behaviour = safety of emediate restart.
 (spin! spindle)
(comment
   (try (spin! spindle)
    (catch Exception e 
              (do 
                 (println "ERROR - spin! failed, stopping spindle")
                 ;(println e)
                 (stop_spindle! spindle)
                 :spindle_errored)))))

(defn start_spindle_thread! [spindle]
^{:doc "Starts the thread that will do the spindling. Does so depending on 
        if there is openCL source attached to the spindle. Will compile and
        run within that context if there is openCL source"}
  (if (not (= (type(@spindle :openCLsource)) java.lang.String))
      (.start (Thread. (fn []
         (while (= (@spindle :spinning?) true)
            (do (safe_spin! spindle)
                (if (= (peek (@spindle :queue)) nil) (Thread/sleep 1))))
         :stoped_spindle)))
      (.start (Thread. (fn []
        (with-cl (with-program (compile-program (@spindle :openCLsource))
           (while (= (@spindle :spinning?) true)
             (do (safe_spin! spindle)
                 (if (= (peek (@spindle :queue)) nil) (Thread/sleep 1))))))
        :stoped_spindle)))))

(defn start_spindle! [spindle]
^{:doc "Checks if sindle is not spinning, if it is not starts the spinning.
        Will start a new thread.
        Prevent the same spindle from being started twice.
        Future feature would be to allow multiple thread to be doing the work.
        However focus is on single ordered workflow"}
  ;;TODO Add a decomission mechanisim, just not running too simple
  ;;TODO write more tests
 (let [ok_to_start_spindle (ref :donno)]
    (dosync
           (if (= (@spindle :spinning?) false)
               (do (ref-set spindle (assoc @spindle :spinning? true))
                   (ref-set ok_to_start_spindle :yes))
               (ref-set ok_to_start_spindle :no)
                  ))
   (if (= @ok_to_start_spindle :yes)
       (start_spindle_thread! spindle)
       :spindle_aready_spinning_somewhere)))

(defn spindle_add_openCLsource! [spindle openCLsource]
^{:doc "Adds openCL source to the spindle. Must be done before spindle is 
        started, else spindle is restarted.
        Spindle must be stoped and started again for new source to take
        effect, which it does on it's own"}
  (dosync 
      (ref-set spindle (assoc @spindle :openCLsource openCLsource)))
  (if (spindle :spinning?)
        (do
          (println "restarting spindle now!")
          (stop_spindle! spindle)
          (Thread/sleep 500)
          (if (@spindle :spinning?)
             (println "not able to stop spindle in time")
             (start_spindle! spindle))    ;;This is effectively broken
          )
        )
  :added_openCL)

(defn is_spindle? [spindle]
;;TODO write test for this
   (:spindle_name @spindle))


(defn sindle_joblag [spindle]
   (let [s @spindle]
     {:jobrequest (:jobid s) :jobtodonext (first (peek (s :queue)))}))


