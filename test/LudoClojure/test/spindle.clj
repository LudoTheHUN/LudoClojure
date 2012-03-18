(ns LudoClojure.test.spindle
  (:use [LudoClojure.spindle])
  (:use [clojure.test])
  )

(comment
This is the old values that was here....
(deftest replace-me ;; FIXME: write
  (is false "No tests have been written."))
)

;Make an empty spindle
(def test_spindle (make_spindle))

(deftest test_empty_spindle
   (is (=  (test_spindle :jobid) 0))
   (is (=  (test_spindle :response) {}))
   (is (=  (peek (test_spindle :queue)) nil))
   )

(deftest test_weave_on!
   (is (= (weave_on! test_spindle #(+ 4 42)) 0))
   (is (= (weave_on! test_spindle #(+ 6 42)) 1))
   )

(deftest test_spindle_contents_after_weave_on
   (is (= (test_spindle :jobid) 2))
   (is (= @((test_spindle :response) 0) :awaiting_response))
   (is (= @((test_spindle :response) 1) :awaiting_response))
   (is (= (first (peek (test_spindle :queue))) 0))
   (is (= ((second (peek (test_spindle :queue)))) (+ 4 42)))
   )

(deftest test_spin_once!
   (is (= (spin_once! test_spindle) [0 46]))
   (is (= (spin_once! test_spindle) [1 48]))
   )

(deftest test_spool_on!
   (is (= (spool_on! test_spindle [0 "some done work"])) "some done work")
   )

(deftest test_spin!
   (weave_on! test_spindle #(+ 43))
   (is (= (spin! test_spindle) 43))
   
   (weave_on! test_spindle #(+ 44))
   (weave_on! test_spindle #(+ 45))
   (is (= (spin! test_spindle) 44))
   (is (= (spin! test_spindle) 45))
   (is (= (spin! test_spindle) :nothing_to_spin))
   )

(deftest test_spool_off!
   (let [jobid (weave_on! test_spindle #(+ 46))]
    (spin! test_spindle)
    (is (= (spool_off! test_spindle jobid)) 46))
   
   (let [jobid2 (weave_on! test_spindle #(+ 47))]
    ;;(spin! test_spindle)   result will not be available yet...
    (is (= (spool_off! test_spindle jobid2) :awaiting_response))
    (spin! test_spindle)
    (is (= (spool_off! test_spindle jobid2) 47))
    (is (= (spool_off! test_spindle jobid2) :response_missing)))
   )


(deftest test_weave_off!
   (let [jobid3 (weave_on! test_spindle #(+ 48))]
     (is (= (weave_off! test_spindle jobid3) :response_timeout))
     (spin! test_spindle)
     (is (= (weave_off! test_spindle jobid3) 48))
     (is (= (weave_off! test_spindle jobid3) :response_missing))))


(deftest test_weave!
    (is (= (weave! test_spindle (fn [] (+ 4 6))) :response_timeout))
    ;;Note: can not get a responce if the spindle is not spinning
    )

;;This forces test to be done in sequence, which is needed here because of the 
;;statefull nature.
(deftest test_spindle_steps
   (test_empty_spindle)
   (test_weave_on!)
   (test_spindle_contents_after_weave_on)
   (test_spin_once!)
   (test_spool_on!)
   (test_spin!)
   (test_spool_off!)
   (test_weave_off!)
   (test_weave!))


(defn test-ns-hook []
  (test_spindle_steps))


