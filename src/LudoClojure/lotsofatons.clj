

;;TODO create a map that holds atoms. Keys are atom name keywords? 
;;The atoms will be maps themselves representing a data structur that defines a nuron
;;Using atoms because a nuron is in only one state at a time, only one operation will be doing a swap, but there could be many reads.


(def a (atom 1))
(swap! a inc)

;;Anon Functions
((fn [r] (inc r)) 4)

;;Anon function over an atom
(swap! a (fn [r] (inc r)))

;;Atom increment with a delay
(swap! a (fn [r] (Thread/sleep 1000) (inc r)))
(deref a)
(deref a)


;;Atom increment with a delay, but done in a non blocking way (instantly returns, does the work on other threads)
(future (swap! a (fn [r] (Thread/sleep 1000) (inc r))))

;Reduce demo
(reduce + [1 2 3 4])

;TODO A map grower with reduce functon
;;help at hand is here: http://www.learningclojure.com/2010/08/reduce-not-scary.html
(reduce (fn [coll item]
           (conj coll item))
 [] (range 10))

;; This gest us close... we jus need keys in the resulting map and corresponging atoms being created..
(reduce (fn [coll item]
           (conj coll {item item}))
 {} (range 10))

(keyword (str "n" 1))
(atom "foo")
(def n1 (atom "foo"))
(def (symbol (str "n" 1)) 4)

;;Map of atoms
(reduce (fn [coll item]
           (conj coll {(keyword (str "n" item)) (atom item)}))
 {} (range 10))

;;TODO These atoms, will chenging them block the map?? will they be readable during a swap!? will they block during a swap?

(def MapOfAtoms 
  (reduce (fn [coll item]
           (conj coll {(keyword (str "n" item)) (atom item)}))
     {} (range 10)))

@(:n2 MapOfAtoms)     ;;same as (deref (:n2 MapOfAtoms))
(swap! (:n2 MapOfAtoms)  inc)
@(:n2 MapOfAtoms)
;;Good, atom within map has changed...
;;Now, what's the blocking story...

;;This obviously blocks...
(swap! (:n2 MapOfAtoms)  (fn [r] (Thread/sleep 1000) (inc r)))
;;This shows that reads are not blocked while atoms is being updated, rest of map is also accessible, however a second swap on :n2 had to retry
(future (swap! (:n2 MapOfAtoms)  (fn [r] (Thread/sleep 10000) (println "about to update atom :n2 with value" r) (inc r))))
(future (swap! (:n3 MapOfAtoms)  (fn [r] (Thread/sleep 20000) (println "about to update atom :n3 with value" r) (inc r))))
(future (swap! (:n2 MapOfAtoms)  (fn [r] (Thread/sleep 5000) (println ":n2 second touch" r) (inc r))))
@(:n2 MapOfAtoms)
;;As long as we can be sure only one swap per atom is happening at any one itme, there will be no retries.


;;TODO what's the cost of pulling things out of a large MapOfAtoms, which are themselves maps...

(time (def LargeMapOfAtoms 
  (reduce (fn [coll item]
           (conj coll {(keyword (str "n" item)) (atom {:type "neuron" :id item :rand_list (range (rand-int 10) (rand-int 10)) }   )}))
     {} (range 40))))
;;; 0.3seconds for    10,000 entries...
;;; 3.6seconds for   100,000 entries...
;;;46.0seconds for 1,000,000 entries...
@(:n2 LargeMapOfAtoms)
(:rand_list @(:n2 LargeMapOfAtoms))
(reduce + (:rand_list @(:n2 LargeMapOfAtoms)))
;reduce map reduce over 
(time (reduce + (map (fn [keysToMap] (reduce + (:rand_list @(keysToMap LargeMapOfAtoms)))) (keys LargeMapOfAtoms))))
;;    20ms for    10,000 things...   500 per ms
;;   100ms for   100,000 things...   1000per ms
;;20,624ms for 1,000,000 things...   This show that we do not have linear time for larger and larger maps???
;;TODO test more if logical fragmentation would make sense (a map of maps, eg: 10 maps with 100k entries each could have returned in 1s and not 20s)



;;What if each neuron held a map of other neurons (synapses) that it was connected to, with some (say for now) random weight
;;TODO support function for creating a randomly connected neuron.
(def MaxNeurons 10000)
;function  that cretes a map of synapses , ie: map of nueron keyword to connection strength.... 
;TODO consider this map to be instead a vector or neuron keyword and strength pairs since we will always want to itterate over these... http://my.safaribooksonline.com/book/programming/clojure/9781935182641/composite-data-types/81
(defn MakeRandomSynapses [NumberOfSynapses]
(reduce (fn [coll item]
          (conj coll {(keyword (str "n" (rand-int MaxNeurons))) (rand)}))
  {} (range NumberOfSynapses)))
(MakeRandomSynapses (rand-int 10))

;;TODO Figure out where is the idiomatics locations for the atom'ization, in the MakeRandomConnectedNuron or in LargeMapOfAtoms (first or 2nd fun below)
(defn MakeRandomConnectedNuron [item]
(atom {:type "neuron" 
       :id item 
       :rand_list (range (rand-int 10) (rand-int 10)) 
       :synapses (MakeRandomSynapses (rand-int 10))}))
(MakeRandomConnectedNuron 10)

(time (def LargeMapOfAtoms 
  (reduce (fn [coll item]
           (conj coll {(keyword (str "n" item)) (MakeRandomConnectedNuron item)}))
     {} (range 10000))))
(:n10 LargeMapOfAtoms)


;;TODO function that reads off the sate of each neuron in the :synapses map of the neuron
;;TODO Add a state to each neuron... in terms of is its firing etc status...

;;TODO a function to update a neuron state based on firing state of neurons on the end of the synapses... (kind of like an echo network)

;;TODO (future) based loop through all the neurons, making updates as above... use a pmap and future? 
  ;;TODO  .. get some visibility into the set of threads gettings spawned



;;TODO Add X Y position for each neuron... move neuron towards center XY of other enurons it is cofiring with....move away from all other neurons?
;;TODO Add a jpanel to see neurons...


