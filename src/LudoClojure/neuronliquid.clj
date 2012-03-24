(ns LudoClojure.neuronliquid)


;; a clojure implentation of this model: http://www.izhikevich.org/publications/spikes.htm
;; http://apprenticepatrick.blogspot.co.uk/2012/01/clojure-records-types-and-protocols.html
(defrecord NeuronliquidRecord [v u i a b c d])

(defprotocol NeuronliquidProtocol
(v_delta [neuron])
(u_delta [neuron])
(i_delta [neuron i])
(flop [neuron])

(pprt [neuron])

  )

(extend-type NeuronliquidRecord NeuronliquidProtocol
  
(v_delta [n]
 (let [v (:v n)
       u (:u n)
       i (:i n)]
  (- (+ (* 0.04 v v) (* 5 v) 140 i) u)))


(u_delta [n]
 (let [v (:v n)
       u (:u n)
       a (:a n)
       b (:b n)]
  (* a (- (* b v) u))))

(i_delta [n i]
 (conj n {:i i}))

(flop [n] ;;TODO make a let here
  (if (> (:v n) 30.0)
      (conj n {:v (:c n) :u (+ (:u n) (:d n))})
      (conj n {:v (+ (:v n) (v_delta n)) :u (+ (:u n) (u_delta n))})))

(pprt [n]
  (str "v:" (format "%.2f" (float(:v n))) " u:" (format "%.2f" (float (:u n)))))

)



(def n (NeuronliquidRecord. 1.0 1.0 10.00 0.02 0.2 -50 2.0))

(class (:v n))
(class (rand 1))


(defrecord NeuronliquidEnsamble [ns cones size])  ;array of NeuronliquidRecord. , connectivity between them, size of ensamble




;;TODO create a model for i (the input signal to be updateble based on other neurons
;;TODO have the input into i be based of an array? or references to the other neurons

(time (defn mk_cone [size connections seed]
  "each connection is an vec of vec of pointer and strength
   DONE TODO make it deterministic via a seed"
    (let [r (java.util.Random. seed)]   ;0 here is the seed
        (vec (map 
               (fn [x] [(long (.nextInt r  size)) (.nextDouble r )]) 
               (range 0 connections))))))


  ;;each connection 

(defn mkNeuronliquidEnsamble [size connections] 
   (NeuronliquidEnsamble. (vec (repeat size n))  (map (fn [seed] (mk_cone size connections seed)) (range size)) size))

(time (def anNeuronliquidEnsamble (mkNeuronliquidEnsamble 1000 50)))

(defn new_i_for_each_n [anNeuronliquidEnsamble]
  (let [cones (:cones anNeuronliquidEnsamble)
        cone (nth (:cones anNeuronliquidEnsamble) 0)
        nuros (:ns anNeuronliquidEnsamble)
        ]
    
    (pmap 
      (fn [cone]
        (reduce (fn [xs x] 
               (+ (* (:v (nth nuros (first x))) 
                     (second x))
                  xs)) 0.0  cone)
        )
      cones)
     )
  )


(new_i_for_each_n anNeuronliquidEnsamble)

(defn do_ensamble_updates []
(time (def d (conj anNeuronliquidEnsamble
  {:ns (vec 
         (map (fn [x y] (conj y {:i x}))
            (new_i_for_each_n anNeuronliquidEnsamble)
            (:ns anNeuronliquidEnsamble)))})))
)

(take 10 (:ns d))

(time (do_ensamble_updates))



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
(time (first nmap))
(time (count bigrun))

;;TODO how to implement connectivity efficently between n's, we need to set thier i value...
;;TODO make this happen on many agent, use clojure parelisim strengths ...

(pprt n)
(def a (map pprt nmap))


;; (println n)
;; (pprt n)
;; 2m neuron updates a second, no synapses!

