(ns LudoClojure.test.core
  (:use [LudoClojure.core])
  (:use [clojure.test]))

(comment
This is the old values that was here....
(deftest replace-me ;; FIXME: write
  (is false "No tests have been written."))
)



(deftest simple-test
  (is (= (hello) "Hello world!"))
  (is (= (hello "test") "Hello test!")))

