(ns LudoClojure.test.core
  (:use [LudoClojure.core])
  (:use [clojure.test])
  (:use [LudoClojure.Win_scrachAlwaysRun]))

(comment
This is the old values that was here....
(deftest replace-me ;; FIXME: write
  (is false "No tests have been written."))
)



(deftest simple-test
  (is (= (hello) "Hello world!"))
  (is (= (hello "test") "Hello test!")))

(deftest Winscrachtest
   (is (map? MapOfKeys))
   (is "Test Note: this test can fail since the random map generated could rouch the same key more then once resulting in less keys"
       (= 30 (count (keys (GrowingKeyMaps 30 {})))))

)

