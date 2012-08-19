(ns LudoClojure.test.core
  (:use [LudoClojure.core])
  (:use [LudoClojure.liquid])
  (:use [clojure.test])
  ;;(:use [LudoClojure.Win_scrachAlwaysRun])
  )


;; TODO move liquid tests to test/liquid.clj 
(comment
This is the old values that was here....
(deftest replace-me ;; FIXME: write
  (is false "No tests have been written."))
)


(deftest liquid-test
   (swap! readout_liquid_status (fn [_] false))
   ;(is (= "Terminating liquid" (run_liquid! globalsize connections sourceOpenCL)))
   (is (= "Terminating liquid" (run_liquid! 64 64 sourceOpenCL)))
   (is (= "Terminating liquid" (run_liquid! 1 1 sourceOpenCL)))
   )

(deftest liquid-test
   (swap! readout_liquid_status (fn [_] true))
   ;(is (= "Terminating liquid" (run_liquid! globalsize connections sourceOpenCL)))
   ;(is (= "Terminating liquid" (run_liquid! (* 64 64) 64 sourceOpenCL)))
   ;(is (= "Terminating liquid" (run_liquid! 1 1 sourceOpenCL)))
   )

(deftest simple-test
  (is (= (hello) "Hello world!"))
  (is (= (hello "test") "Hello test!")))

;(deftest Winscrachtest
;   (is (map? MapOfKeys))
;   (is "Test Note: this test can fail since the random map generated could rouch the same key more then once resulting in less keys"
;       (= 30 (count (keys (GrowingKeyMaps 30 {})))))
;
;)

