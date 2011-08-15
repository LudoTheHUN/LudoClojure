;scratch3

;TODO Broke, but would be nice to know sizes of things...

(ns LudoClojure.core
  (:gen-class)
  (:require clojure.contrib.seq-utils)
  (:require clojure.contrib.probabilities.random-numbers)  ;;gives lcg random stream generator
  (:require clojure.contrib.duck-streams )
  (:gen-class))


(defn gc []
  (dotimes [_ 4] (System/gc)))

(defn used-memory []
  (let [runtime (Runtime/getRuntime)]
    (gc)
    (- (.totalMemory runtime) (.freeMemory runtime))))

(defn measure [f]
  (let [before (used-memory)
        _ (def foo (clojure.contrib.duck-streams/with-in-reader f (read)))
        after (used-memory)]
    (- after before)))


(def z (myCLJarrayFromJavaArrayFun  (GrabScreenColorJavaArray  0 0 x y)))
(measure z)







