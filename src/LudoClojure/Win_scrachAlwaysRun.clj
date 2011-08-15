(ns user
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
