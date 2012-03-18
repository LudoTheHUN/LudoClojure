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
   

;;This forces test to be done in sequence, which is needed here because of the 
;;statefull nature.
(deftest test_spindle_steps
   (test_empty_spindle)
   (test_weave_on!)
   (test_spindle_contents_after_weave_on)
   (test_spin_once!)
   (test_spool_on!))

(defn test-ns-hook []
  (test_spindle_steps))


