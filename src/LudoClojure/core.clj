(ns LudoClojure.core
  (:use LudoClojure.Win_scrachAlwaysRun)
  (:gen-class))




(println "hello there from core.clj file")

(defn hello
  ([] "Hello world!")
  ([name] (str "Hello " name "!")))


(defn -main [& args]
  (println "Its not easy enough to get started with Clojure programming")
(println (hello args))
)



