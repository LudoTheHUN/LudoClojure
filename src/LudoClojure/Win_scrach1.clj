(ns user
  (:use [clojure.contrib.math]))

(deserialize "C:/Users/Ludo/Desktop/alwaysRun.clj")
clojure.lang.Compiler.loadFile("C:/Users/Ludo/Desktop/alwaysRun.clj");

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


(first (keys MapOfKeys))

(defn MapKeyTravelSafe [aKeyKeyMap entryKey maxjumps]
    (loop [aKeyKeyMapLoop aKeyKeyMap entryKeyLoop entryKey maxjumpsLoop maxjumps]
    (println entryKeyLoop (aKeyKeyMapLoop entryKeyLoop) maxjumpsLoop)
      (if (or (nil? (aKeyKeyMapLoop entryKeyLoop)) (> 0 maxjumpsLoop))
         {entryKeyLoop maxjumpsLoop}
        (recur aKeyKeyMapLoop (aKeyKeyMapLoop entryKeyLoop) (dec maxjumpsLoop)))))


(MapKeyTravelSafe MapOfKeys :at 100)
(MapKeyTravelSafe MapOfKeys (first (keys MapOfKeys)) 100)

;Now find the longest travel
(def longestEndPoints (map (fn [x] (MapKeyTravelSafe MapOfKeys x 100)) (keys MapOfKeys)))
longestEndPoints  ;Note, output here is a LazySeq!!!
;!!! note that longestEndPoints gets computed on first execution!?!?!!


(time (reduce + (buildRandArraySafe 1000000 [])))
(time (reduce + (memRanArrBuild 1000000 [])))

;;Interesting behaviour... memoized version had to recompute after it got redefined ...
(take 10 (buildRandArraySafe 1000000 []))
(take 10 (memRanArrBuild 1000000 []))
(def memRanArrBuild (memoize buildRandArraySafe))
(take 10 (memRanArrBuild 1000000 []))
; Indeed, it did reset...



;create many keywords (neuron 'names' so they can be looked up in hashmaps)




(def fooMap (GrowingMaps 2000 {}))
(def fooKeys (keys fooMap))
(take 10 fooKeys)
(count (sort fooKeys))

(fooMap (take 1 fooKeys))
(fooMap :TS)
;doing fast lookup on a set of keys
(select-keys fooMap [:a0 :b1 :cc])
;find a subset of n keys...
(select-keys fooMap (take 10 fooKeys))

;map of maps
{:a {:a :b :b 1}}
;accessing a map of maps
(({:a {:a :b :b 1}} :a) :a)
;can we make that conditional?
;keep traversing the map untill you find an leaf..., could get into loops, but not if we memorise which nodes we saw.



;instantanious? mapkeytraversal.. a model for a synapse?
(take 1 (sort(keys MapOfKeys)))
;Take the value of the first key
(MapOfKeys :00)
(apply MapOfKeys (take 1 (sort(keys MapOfKeys))))
(MapOfKeys :0R)
(apply MapOfKeys (take 1 (sort(keys MapOfKeys))))
(MapOfKeys :E7) ....etc

(MapOfKeys (MapOfKeys (apply MapOfKeys (take 1 (sort(keys MapOfKeys))))))
(MapOfKeys (MapOfKeys (MapOfKeys (apply MapOfKeys (take 1 (sort(keys MapOfKeys)))))))
(MapOfKeys (MapOfKeys (MapOfKeys (MapOfKeys (apply MapOfKeys (take 1 (sort(keys MapOfKeys))))))))

(reduce MapOfKeys [(apply MapOfKeys (take 1 (sort(keys MapOfKeys))))])
;;The idea is to very quickly trace through a sub set within the internal 
;Trampoline?
(MapOfKeys :00)
(MapOfKeys (MapOfKeys :00))
(MapOfKeys (MapOfKeys (MapOfKeys :00)))
(MapOfKeys (MapOfKeys (MapOfKeys (MapOfKeys :00))))
(MapOfKeys (MapOfKeys (MapOfKeys (MapOfKeys (MapOfKeys :00)))))
;finding sub sequnces.... WIP

(defn MapKeyTravel [aKeyKeyMap entryKey maxjumps]
    (if (and (nil? (aKeyKeyMap entryKey)) (> 0 maxjumps))
      entryKey
      (MapKeyTravel aKeyKeyMap (aKeyKeyMap entryKey) (dec maxjumps))))
;first
(take 1 (sort(keys MapOfKeys)))
;one level down
(apply MapOfKeys (take 1 (sort(keys MapOfKeys))))
;two levels down
(MapOfKeys (apply MapOfKeys (take 1 (sort(keys MapOfKeys)))))

(MapKeyTravel MapOfKeys :09 10)




(defn MapKeyTravelSafe [aKeyKeyMap entryKey maxjumps]
    (loop [aKeyKeyMapLoop aKeyKeyMap entryKeyLoop entryKey maxjumpsLoop maxjumps]
    (println entryKeyLoop (aKeyKeyMapLoop entryKeyLoop) maxjumpsLoop)
      (if (and (nil? (aKeyKeyMapLoop entryKeyLoop)) (< 0 maxjumpsLoop))
         entryKeyLoop
        (recur aKeyKeyMapLoop (aKeyKeyMapLoop entryKeyLoop) (dec maxjumpsLoop)))))

(MapKeyTravelSafe MapOfKeys :at 100)


(take 10 (sort MapOfKeys))


(defn buildRandArraySafe [n array]
  (loop [k n arrayGrow array]
    (if (= k 0)
      arrayGrow
      (recur (dec k) (conj arrayGrow (rand 1))))))

;;;We could use this in reverse to find end points on the traversal

(conj {} {(keyword (random-string 6)) (rand 1)})


(def mapgrowingGrow {})
(conj mapgrowingGrow {:foo (rand 1)})



(random-string 123) 


(keys (conj mapgrowingGrow {:foo (rand 1)}))
(time (do (range 1000000) nil))



;; how to do a subselect from the 1-1000 range postante?
(do (for [x (range 0 1000) :when (even? x)] x))
(macroexpand '(for [x (range 0 1000) :when (even? x)] x))

(defn myfilter [x]
    (if (< x 100)
       true
       nil))
(for [x (range 0 1000) :when (even? x)] x)
(for [x (range 0 10000000) :when (myfilter x)] x)
(myfilter 10)
(time (reduce + (for [x (range 0 10000000) :when (myfilter x)] x)))


(keyword "foo")
(def myVector [1 23 43 123])
(myVector 2)

(map even? (range 0 1000 2))
(time (reduce + (range 0 4000000 2)))

;Ruducing non associative data? (so not like plus...)


(defn buildRandArray [n array]
  (if (= n 0)
   array
   (buildRandArray (dec n) (conj array (rand 1)))))
   
(buildRandArray 4 [])
(def foo1 (buildRandArray 4 []))
(foo1 2)
(time (reduce + (buildRandArray 10000 [])))



(defn buildRandArraySafe [n array]
  (loop [k n arrayGrow array]
    (if (= k 0)
      arrayGrow
      (recur (dec k) (conj arrayGrow (rand 1))))))

(time (def RandArray (buildRandArraySafe 1000000 [])))

(time (reduce + (buildRandArraySafe 1000000 [])))

(time (count RandArray))
(time (count (sort RandArray)))
(def RandArraySorted (sort RandArray))
(time (reduce + RandArray))
(take 6 RandArray)
(take 6 RandArraySorted)
(count RandArraySorted)
(nth RandArraySorted 600)

((buildRandArraySafe 10 []) 4)

(ns foo
  (:use [clojure.contrib.math ]))
(round 10.12 1)

(for [x (range 1 5)
      y (range 0 x)]
  [x y])

(def agentx (agent 100))

(def somestring "how are you today")

(seq somestring)

(reduce 
   (fn [m k] (update-in m [k] #(if (nil? %) 1 (inc %))))
   {}
   (seq somestring))
