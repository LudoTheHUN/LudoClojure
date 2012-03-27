(ns LudoClojure.neuronliquid)


;; a clojure implentation of this model: http://www.izhikevich.org/publications/spikes.htm
;; http://apprenticepatrick.blogspot.co.uk/2012/01/clojure-records-types-and-protocols.html


;;;TODO type hint everything, see performance boost
;;;TODO reimplement with connectivity within the NeuronliquidRecord
(defrecord NeuronliquidRecord [^Double v
                               ^Double u
                               ^Double i
                               ^Double a
                               ^Double b
                               ^Double c
                               ^Double d])

(defprotocol NeuronliquidProtocol
(v_delta [neuron])
(v_delta_with_i [neuron i])
(u_delta [neuron])
(i_delta [neuron i])
(flop [neuron])
(flop_with_i [n i])

(pprt [neuron])

  )

(extend-type NeuronliquidRecord NeuronliquidProtocol
  
(v_delta [n]
 (let [v (:v n)
       u (:u n)
       i (:i n)]
  (- (+ (* 0.04 ^Double v ^Double v) (* 5.0 ^Double v) 140 ^Double i) ^Double u)))

(v_delta_with_i [n i]
 (let [v (:v n)
       u (:u n)]
  (- (+ (* 0.04 ^Double v ^Double v) (* 5.0 ^Double v) 140 ^Double i) ^Double u)))


(u_delta [n]
 (let [v (:v n)
       u (:u n)
       a (:a n)
       b (:b n)]
  (* ^Double a (- (* ^Double b ^Double v) ^Double u))))

(i_delta [n i]
 (conj n {:i i}))

(flop [n] ;;TODO make a let here
  (if (> (:v n) 30.0)
      (conj n {:v (:c n) :u (+ (:u n) (:d n))})
      (conj n {:v (+ (:v n) (v_delta n)) :u (+ (:u n) (u_delta n))})))

(flop_with_i [n i] ;;TODO make a let here
  (if (> (:v n) 30.0)
      (conj n {:v (:c n) :u (+ (:u n) (:d n))})
      (conj n {:v (+ (:v n) (v_delta_with_i n i)) :u (+ (:u n) (u_delta n))})))

(pprt [n]
  (str "v:" (format "%.2f" (float(:v n))) " u:" (format "%.2f" (float (:u n)))))

)




(defn new_i_for_each_n [anNeuronliquidEnsamble]
  "takes NeuronliquidEnsamble and returns array of new i's for each  "
  (let [cones (:cones anNeuronliquidEnsamble)
        nuros (:neurs anNeuronliquidEnsamble)]
    (pmap 
      (fn [cone]
        (reduce (fn [xs x] 
                    (+ (* (:v (nth nuros (first x)))(second x))
                       xs)) 0.0  cone))
     cones)))

(def n (NeuronliquidRecord. 1.0 1.0 10.00 0.02 0.2 -50.0 2.0))

(class 1)
(class 1.0)
(class (:v n))
(class (:c n))
(class (rand 1))


(defrecord NeuronliquidEnsamble [^clojure.lang.PersistentVector neurs      ;array of NeuronliquidRecord.
                                 ^clojure.lang.PersistentVector cones   ;array of connectivity specs between ns, by array positon
                                 ^Integer size])  ;array of NeuronliquidRecord. , connectivity between them, size of ensamble

(defprotocol NeuronliquidEnsambleProtocol
  (compute_i_vec [ensamble])
  (ensamble_flop [ensamble])   ;protol based ivec cals and update-in
  (ensamble_flop2 [ensamble])  ; external function based ivec calc and conj
  )

(extend-type NeuronliquidEnsamble NeuronliquidEnsambleProtocol
 (compute_i_vec [ensamble]
    (let [cones (:cones ensamble)
          nuros (:neurs ensamble)]
    (pmap 
      (fn [cone]
        (reduce (fn [xs x] 
                    (+ (* (:v (nth nuros (first x))) (second x))
                   xs)) 0.0  cone)
        )
      cones)
     )
    )

 (ensamble_flop [ensamble]
  (let [new_i_vec (compute_i_vec ensamble)        ]
    (update-in ensamble [:neurs] 
               (fn [neurs] (vec (map flop_with_i neurs new_i_vec))))
    ))
 
 (ensamble_flop2 [ensamble]
   (let [new_is (new_i_for_each_n ensamble)]
     (conj ensamble
       {:neurs
         (vec (map flop_with_i (:neurs ensamble) new_is))
            })))
)



  

(quote ;;DROP old implementation
  (ensamble_flop [ensamble]
  (let [new_i_vec (compute_i_vec ensamble)
        neurs (:neurs ensamble)]
    (conj ensamble {:neurs
         (vec (map flop_with_i neurs new_i_vec))
            })
    )))


;;TODO create a model for i (the input signal to be updateble based on other neurons
;;TODO have the input into i be based of an array? or references to the other neurons

(time (defn mk_cone [size connections seed]
  "each connection is an vec of vec of pointer and strength
   DONE TODO make it deterministic via a seed"
    (let [r (java.util.Random. seed)]   ;0 here is the seed
        (vec (map 
               (fn [x] [(long (.nextInt r  size)) (.nextDouble r )]) 
               (range 0 connections))))))

(mk_cone 10 2 0)


  ;;each connection 

(defn mkNeuronliquidEnsamble [size connections] 
   (NeuronliquidEnsamble. (vec (repeat size n))  
                          (map (fn [seed] (mk_cone size connections seed)) (range size)) 
                          size))

(time (def anNeuronliquidEnsamble (mkNeuronliquidEnsamble 100 50)))


(* 100000 10 (/ 1000.0 350 ) 10)
(* 100 10 (/ 1000.0 5 ) 10)
(class (:neurs anNeuronliquidEnsamble))


(time (def somenew_is (new_i_for_each_n anNeuronliquidEnsamble)))
(take 20 somenew_is)

(defn do_ensamble_updates [anNeuronliquidEnsamble]
 (conj anNeuronliquidEnsamble
  {:neurs 
         (vec (map (fn [x y] (conj y {:i x}))
            (new_i_for_each_n anNeuronliquidEnsamble)
            (:neurs anNeuronliquidEnsamble)))}))


(defn do_ensamble_flop [anNeuronliquidEnsamble]
(let [new_is (new_i_for_each_n anNeuronliquidEnsamble)]
 (conj anNeuronliquidEnsamble
  {:neurs 
       (vec (map flop_with_i (:neurs anNeuronliquidEnsamble) new_is))
            })))

(defn flop_n_times [flopfunction thingtoflop repeats]
 (loop   [thingtoflop thingtoflop repeats repeats] 
  (if (= repeats 0)
    thingtoflop
    (recur (flopfunction thingtoflop) (- repeats 1)))))



(time (last (:neurs (flop_n_times    ensamble_flop2 anNeuronliquidEnsamble 700))))   ;;520ms
(time (last (:neurs (flop_n_times    ensamble_flop  anNeuronliquidEnsamble 700))))   ;;540ms
(time (last (:neurs (flop_n_times do_ensamble_flop  anNeuronliquidEnsamble 700))))   ;;520ms

(time (flop_n_times flop  n 700))   ;;520ms






(class (:neurs (time (do_ensamble_updates anNeuronliquidEnsamble))))
(take 4 (:neurs (time (do_ensamble_updates anNeuronliquidEnsamble))))
(reduce (fn [xs x] (conj xs x)) [] [1 2 3])
(conj [1 2 3] 4)
  ;(i_delta n i)
(v_delta n)
(u_delta n)
(flop n)
(-> n flop flop)
(-> n flop flop flop)
(defn run_times [n repeats]
 (loop [n n repeats repeats] 
  (if (= repeats 0)
    n
    (recur (flop n) (- repeats 1)))))

(time (run_times n 100000))
(time (def nmap_sizescaling0  (repeat 10000 n)))
(time (def nmap_sizescaling1  (vec nmap_sizescaling0)))
(time (def nmap_sizescaling (vec (repeat 10000 n))))  ;;1k neurons
(class nmap_sizescaling)
(time (nth nmap_sizescaling 9999))
(time (count nmap_sizescaling))
(time (last nmap_sizescaling))
(time (first nmap_sizescaling))

(defn p_run_times_fast [nmap repeats]
 (loop [nmap nmap repeats repeats] 
  (if (= repeats 0)
         nmap
         (recur (map flop nmap) (- repeats 1)))))
(time 
  (let [simed (p_run_times_fast nmap_sizescaling 100)]
  (println (class simed))
  (time (doall (println (pprt(last simed))) (println :done)))))

;(time (count (p_run_times_fast nmap 1000)))
(time (def bigrun (p_run_times_fast nmap_sizescaling 1000)))   ;floped 1k times
(class bigrun)
(nth bigrun 1000)
(time (first bigrun))
(time (nth bigrun 500))
(time (nth bigrun 998))
;(time (first nmap))
(time (count bigrun))
;;TODO how to implement connectivity efficently between n's, we need to set thier i value...
;;TODO make this happen on many agent, use clojure parelisim strengths ...
(pprt n)
;(def a (map pprt nmap))


;; (println n)
;; (pprt n)
;; 2m neuron updates a second, no synapses!

