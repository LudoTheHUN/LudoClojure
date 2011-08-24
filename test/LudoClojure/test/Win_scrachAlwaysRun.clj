(ns LudoClojure.test.Win_scrachAlwaysRun
  (:use [clojure.test])
  (:use [LudoClojure.Win_scrachAlwaysRun]))


(deftest Winscrachtest_underOwnTestFile
   (is (map? MapOfKeys))
   (is "Test Note: this test can fail since the random map generated could rouch the same key more then once resulting in less keys, this is it show what would happen if a test did fail then anything else..."
       (= 100 (count (keys (GrowingKeyMaps 100 {})))))

)

