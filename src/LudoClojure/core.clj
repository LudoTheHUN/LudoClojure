(ns LudoClojure.core
  (:gen-class))




(println "hello there from core.clj file")

(defn hello
  ([] "Hello world!")
  ([name] (str "Hello " name "!")))


(defn -main [& args]
  (println "you people dont make it easy to get started with programming")
(println (hello args))
)



