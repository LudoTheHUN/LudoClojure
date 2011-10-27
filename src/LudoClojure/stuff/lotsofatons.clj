

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
;(MakeRandomSynapses (rand-int 10))

;;TODO Figure out where is the idiomatics locations for the atom'ization, in the MakeRandomConnectedNuron or in LargeMapOfAtoms (first or 2nd fun below)
(defn MakeRandomConnectedNuron [item]
(atom {:itemtype "neuron" 
       :id item
       :firestate (rand-int 3)
       :rand_list (range (rand-int 10) (rand-int 10)) 
       :synapses (MakeRandomSynapses (rand-int 100))}))
(MakeRandomConnectedNuron 10)

(time (def LargeMapOfAtoms
  (reduce (fn [coll item]
           (conj coll {(keyword (str "n" item)) (MakeRandomConnectedNuron item)}))
     {} (range MaxNeurons))))
(:n10 LargeMapOfAtoms)

;;TODO function that reads off the sate of each neuron in the :synapses map of a neuron
;;Access function for neuron values
(defn GetNeuronValue [aMapOfAtoms neuronKey keywordtolookup]
    (keywordtolookup (deref(neuronKey aMapOfAtoms))))
;(GetNeuronValue LargeMapOfAtoms :n10 :synapses)
;(GetNeuronValue LargeMapOfAtoms :n10 :firestate)



;;;;;;;;;;;;;;;;;;;;;; Building the state update function, one substatement at a time;;;;;;;;;;;;;;;;;
;;TODO Add a state to each neuron... in terms of is its firing etc status...

;;TODO a function to update a neuron state based on firing state of neurons on the end of the synapses... (kind of like an echo network)
;eg:
;;(swap! (:n3 LargeMapOfAtoms)  ;The atom 
      ;; (fn [r] (Thread/sleep 1000) (inc r))) ;The update functions which accepts the atom value and returns the new value for the whole atom
;(swap! (:n5 LargeMapOfAtoms)
;       (fn [Neuron] (conj Neuron {:firestate 0})))
;;OK now just need a function to replace the 0 above which will be based on synapses and firestates of other neurons...
; 
(def synapsetrengths (GetNeuronValue LargeMapOfAtoms :n10 :synapses))  ;; given this list of nuron keys, synapse strenths, look up those neuron firestates...
;; we will reduce over the set of keys in the synapse set, however we will both lookup the synapse strength and the firestate at the neuron keyword (or do any other work that needs to done at this time)
;;a very verbose reduce would be...
(reduce
  (fn [coll item]
    (conj coll {item [(item synapsetrengths) (GetNeuronValue LargeMapOfAtoms item :firestate)]}))
  {} (keys synapsetrengths))
;;but we want just a single int in the [0 1 2] set... so.   ;;; TODO This needs much more neurology insight to be usful  
(quot
(reduce
  (fn [coll item]
    (+ coll (* (item synapsetrengths) (GetNeuronValue LargeMapOfAtoms item :firestate))))
  0 (keys synapsetrengths))
(count synapsetrengths))
;;; And as a more proper function...

(defn updatefirestate [aMapOfAtoms neuronKey]
  (let [synapsetrengths (GetNeuronValue aMapOfAtoms neuronKey :synapses)
        LookupKeys      (keys synapsetrengths)
        LookupKeysCount (count synapsetrengths)]
;;START of nurologically v poor function for determining a new state
    (if 
      (= LookupKeysCount 0) 0
      (/ (reduce
            (fn [coll item]
              (+ coll (* (item synapsetrengths) (GetNeuronValue aMapOfAtoms item :firestate))))
             0 LookupKeys) LookupKeysCount ))))
;;END of nurologically v poor funciton
;(updatefirestate LargeMapOfAtoms :n20)

;;Putting this into the neuron swap function....
(swap! (:n37 LargeMapOfAtoms)
       (fn [Neuron] (conj Neuron {:firestate (updatefirestate LargeMapOfAtoms :n37)})))


(future (swap! (:n37 LargeMapOfAtoms)
        (fn [Neuron] (conj Neuron {:firestate (updatefirestate LargeMapOfAtoms :n37)}))))

;Now apply updates to firestate over all neurons...
;;;;map is lazy...
(def m 
  (map 
    (fn [x]
      (print x " boo")
      x)
    [1 2 3] ))
(dorun m)


(defn updateAllfirestate1! "using dorun and map, each neuron update is pushed to a future" [aMapOfAtoms]
  (let [atomkeys (keys aMapOfAtoms)]
    (dorun (map 
       (fn [x] (future (swap! (x aMapOfAtoms)
                         (fn [Neuron] (conj Neuron {:firestate (updatefirestate aMapOfAtoms x)})))))
             atomkeys))))

(defn updateAllfirestate2! "same as 1 but not pushing into a future, seems faster..." [aMapOfAtoms]
  (let [atomkeys (keys aMapOfAtoms)]     ;;could optimise by making this an atom that's already available
    (dorun (map 
       (fn [x] (swap! (x aMapOfAtoms)
                         (fn [Neuron] (conj Neuron {:firestate (updatefirestate aMapOfAtoms x)}))))
             atomkeys))))

(defn updateAllfirestate3! "using the loop recur patern without future" [aMapOfAtoms]
  (let [atomkeys (keys aMapOfAtoms)
        atomscount (count atomkeys)]     ;;could optimise by making this an atom that's already available
    (loop [i 0]
      (if (= i atomscount)
          (println "doneupdate")
          (do (let [x (nth atomkeys i)]
                   (swap! (x aMapOfAtoms)
                     (fn [Neuron] (conj Neuron {:firestate (updatefirestate aMapOfAtoms x)}))))
            (recur (inc i)))))))

(defn updateAllfirestate4! "using the loop recur patern with future" [aMapOfAtoms]
  (let [atomkeys (keys aMapOfAtoms)
        atomscount (count atomkeys)]     ;;could optimise by making this an atom that's already available
    (loop [i 0]
      (if (= i atomscount)
          (println "doneupdate")
          (do (let [x (nth atomkeys i)]
                   (future (swap! (x aMapOfAtoms)
                     (fn [Neuron] (conj Neuron {:firestate (updatefirestate aMapOfAtoms x)})))))
            (recur (inc i)))))))

(defn updateAllfirestate5! "using a doseq over the keys of the atommap" [aMapOfAtoms]
     (doseq [i (keys aMapOfAtoms)]
       ((fn [x] (swap! (x aMapOfAtoms)
                         (fn [Neuron] (conj Neuron {:firestate (updatefirestate aMapOfAtoms x)}))))    ;;TODO it's could be possible to avoid one round of map lookups by using (vals aMapOfAtoms)
        i)))

(defn updateAllfirestate6! "using a doseq over the keys of the atommap , passing individual operations into futures" [aMapOfAtoms]
     (doseq [i (keys aMapOfAtoms)]
       ((fn [x] (future (swap! (x aMapOfAtoms)
                          (fn [Neuron] (conj Neuron {:firestate (updatefirestate aMapOfAtoms x)})))))
        i)))

(defn updateAllfirestate7! "using a doseq over the keys of the atommap, but putting everything into a future at the start" [aMapOfAtoms]
     (future (doseq [i (keys aMapOfAtoms)]
       ((fn [x] (future (swap! (x aMapOfAtoms)
                          (fn [Neuron] (conj Neuron {:firestate (updatefirestate aMapOfAtoms x)})))))
        i))))


;;TODO  rewrite all map scans with a doseq as per  http://java.ociweb.com/mark/clojure/article.html

(count LargeMapOfAtoms)
(time (updateAllfirestate1! LargeMapOfAtoms))        ;
(time (updateAllfirestate2! LargeMapOfAtoms))   ;work only takes 57ms on 10k?
(time (updateAllfirestate3! LargeMapOfAtoms))
(time (updateAllfirestate4! LargeMapOfAtoms))
(time (updateAllfirestate5! LargeMapOfAtoms))    ;; this is the fastest at 10k neurons 10 avg connections, done in 50ms
(time (updateAllfirestate6! LargeMapOfAtoms))    ;; overhead of starting futures is not worth it here at 10k neurons 10avg synapses.
(time (updateAllfirestate7! LargeMapOfAtoms))

;;TODO NOTE this is all flawed because each neuron is seeing half updated half pending update world state, however, that's ok, becuase we
;;;will have a continuous time model, with neuron states being reupdated and checked vs one global time, with many threads running over 
;;;the neurons making updates. So we will know the last time the nuron fired at each check, and the last time it was 'refrshed' (that is,
;;;last time we checked if it should be firing, we can even 
;;;initiate a refresh if we see it as being too long ago (eg: more then 100ms). To check if it should fire now....

;;;TODO get a better neron model!

;;See what the values are before hand...
(map (fn [atomKey] (GetNeuronValue LargeMapOfAtoms atomKey :firestate)) (map (fn [x] (nth (keys LargeMapOfAtoms) x)) (range 10)))
(GetNeuronValue LargeMapOfAtoms :n0 :firestate)

;;Then make another update....
(updateAllfirestate! LargeMapOfAtoms)




(def slurped_future 
  (future 
    (slurp
      "http://google.com")))
(count @slurped_future)

;;TODO (future) based loop through all the neurons, making updates as above... use a pmap and future? 
  ;;TODO  .. get some visibility into the set of threads gettings spawned



;;TODO Add X Y position for each neuron... move neuron towards center XY of other enurons it is cofiring with....move away from all other neurons?
;;TODO Add a jpanel to see neurons...


;;TODO consider liquid computing:  http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.131.277&rep=rep1&type=pdf


