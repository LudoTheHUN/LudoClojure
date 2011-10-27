
;;To run in windows do something like:
;;::#java -jar C:\LACIE_Copy\Clojure\clojure-1.2.1\clojure-1.2.1\clojure.jar
;;::java.exe -cp "C:\LACIE_Copy\Clojure\clojure-1.2.1\clojure-1.2.1\clojure.jar;C:\LACIE_Copy\Clojure\clojure-contrib-1.2.0\target\clojure-contrib-1.2.0.jar" clojure.main
;;java.exe -cp "C:\LACIE_Copy\Clojure\clojure-1.2.1\clojure-1.2.1\clojure.jar;C:\LACIE_Copy\Clojure\clojure-contrib-1.2.0\target\clojure-contrib-1.2.0.jar" clojure.main -i C:\Users\Ludo\Desktop\alwaysRun.clj -r


;;Note the namespace works better... just need to have the same napespace as the file.clj (when under lein), from the src/ and up, so need LudoCojure here because the's the subfolder
(ns LudoClojure.Win_scrachAlwaysRun
  (:use [clojure.contrib.math ]))

(defn buildRandArraySafe [n array]
  (loop [k n arrayGrow array]
    (if (= k 0)
      arrayGrow
      (recur (dec k) (conj arrayGrow (rand 1))))))

(defn random-string [length]
  (let [ascii-codes (concat (range 48 58) (range 66 91) (range 97 123))]
  (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

(defn GrowingMaps [n mapgrowing]
  (loop [k n mapgrowingGrow mapgrowing]
    (if (= k 0)
      mapgrowingGrow
      (recur (dec k) (conj mapgrowingGrow {(keyword (random-string 2)) (rand 1)}  )))))

;Grow a key to key map based on random words
(defn GrowingKeyMaps [n mapgrowing]
  (loop [k n mapgrowingGrow mapgrowing]
    (if (= k 0)
      mapgrowingGrow
      (recur (dec k) (conj mapgrowingGrow {(keyword (random-string 2)) (keyword (random-string 2))}   )))))

(def memRanArrBuild (memoize buildRandArraySafe))

(def MapOfKeys (GrowingKeyMaps 100 {}))


(println "Always Run has been loaded")
