
(comment Make Java move the mouse to x y location)
(.mouseMove (java.awt.Robot.) 10 23)

(comment lets see how quickly this can itterate)


(def countingdown1 (fn [n]
                    (println n)  n))
(countingdown1 10)


(def countingdown2 (fn [n]
                     (println n) 
                     (- n 1)))
(countingdown2 10)

(comment this may make it fun to run off)
(defn countingdown3 (fn [n]
                     (println n)
                     (Thread/sleep 1000) 
                     (.mouseMove (java.awt.Robot.) n n)
                     (countingdown3 (- n 1) )))
(countingdown3 500)


(defn countingdown4 [n]
  (if (< n 1) (println "all done")
                     (do (println n)
                      (comment (Thread/sleep 1) 
                       (.mouseMove (java.awt.Robot.) n n))
                       (countingdown4 (- n 1) ))))
(countingdown4 50000)
(comment what's with the stack overflow!!)


(defn countingdown5 [n]
  (loop (if (< n 1) (println "all done")
                     (do (println n)
                      (comment (Thread/sleep 1) 
                       (.mouseMove (java.awt.Robot.) n n))
                       (recur(countingdown5 (- n 1) ))))))
(countingdown5 50000)
(comment WIP this still fails)

(defn miniloop [k]
   (loop [n k]
       (if (= n 1)
           n
           (do (println "hello fo" n)
             (recur (dec n))))))
           
(miniloop 100)

(comment Java interop)
(. (. System (getProperties)) (get "os.name"))
(. System (nanoTime))



    
(def factorial
  (fn [n]
    (loop [cnt n acc 1]
       (if (zero? cnt)
            acc
          (recur (dec cnt) (* acc cnt))))))
(factorial 10)      
         
