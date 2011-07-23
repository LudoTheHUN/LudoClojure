(ns LudoClojure.core
  (:gen-class))

(defn -main [& args]
  (println "you people dont make it easy to get started with programming"))


(+ 4 5)
(println "hello there from core.clj file")

(defn hello
  ([] "Hello world!")
  ([name] (str "Hello " name "!")))
