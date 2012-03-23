(ns LudoClojure.neuronliquid)


;; a clojure implentation of this model: http://www.izhikevich.org/publications/spikes.htm
;; http://apprenticepatrick.blogspot.co.uk/2012/01/clojure-records-types-and-protocols.html
(defrecord NeuronliquidRecord [v u i a b c d])

(defprotocol NeuronliquidProtocol
(v_delta [neuron])
(u_delta [neuron])
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

(flop [n] ;;TODO make a let here
  (if (> (:v n) 30.0)
      (conj n {:v (:c n) :u (+ (:u n) (:d n))})
      (conj n {:v (+ (:v n) (v_delta n)) :u (+ (:u n) (u_delta n))})))

(pprt [n]
  (str "v:" (format "%.2f" (float(:v n))) " u:" (format "%.2f" (float (:u n)))))

)



(quote
(defprotocol NeuronsliquidProtocol  ;Protocol for a collection of neurons
(v_delta [neurons])
(u_delta [neurons])
(flop [neurons])
  )
)


(def n (NeuronliquidRecord. 1.0 1.0 10.00 0.02 0.2 -50 2.0))

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

(time (run_times n 10000))


(def nmap (repeat 1000 n))  ;;1k neurons


(defn p_run_times_fast [nmap repeats]
 (loop [nmap nmap repeats repeats] 
  (if (= repeats 0)
         nmap
         (recur (map flop nmap) (- repeats 1)))))


(time 
  (let [simed (p_run_times_fast nmap 1000)]
  (time (doall (println (pprt(last simed))) (println :done)))))
(time (count (p_run_times_fast nmap 1000)))

(time (def bigrun (p_run_times_faster nmap 1000)))   ;floped 1k times
(class bigrun)



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

