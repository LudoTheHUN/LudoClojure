(ns LudoClojure.neuronliquid)


;; a clojure implentation of this model: http://www.izhikevich.org/publications/spikes.htm
;; http://apprenticepatrick.blogspot.co.uk/2012/01/clojure-records-types-and-protocols.html
(defrecord NeuronliquidRecord [v u i a b c d])

(defprotocol NeuronliquidProtocol
(v_delta [neuron])
(u_delta [neuron])
(flop [neuron])
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

(flop [n]
  (if (> (:v n) 30.0)
      (conj n {:v (:c n) :u (+ (:u n) (:d n))})
      (conj n {:v (v_delta n) :u (u_delta n)})))

)



(defprotocol NeuronsliquidProtocol  ;Protocol for a collection of neurons
(v_delta [neurons])
(u_delta [neurons])
(flop [neurons])
  )



(def n (NeuronliquidRecord. 1.0 1.0 1.0 0.02 0.2 -50 2.0))

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

(time (run_times n 10001))


(def nmap (vec (repeat 10000 n)))

(defn p_run_times [nmap repeats]
 (loop [nmap nmap repeats repeats] 
  (if (= repeats 0)
    nmap
    (recur (vec (map flop nmap)) (- repeats 1)))))

(time (doall (p_run_times nmap 20) (println :done)))


;; 2m neuron updates a second, no synapses!

