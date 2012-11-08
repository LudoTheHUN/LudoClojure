(ns LudoClojure.protocols-example)




(defprotocol P
  (foo [x])
  (bar-me [x] [x y]))
 
(deftype Foo [a b c]
  P
  (foo [x] a)
  (bar-me [x] b)
  (bar-me [x y] (+ c y)))
 
(bar-me (Foo. 1 2 3) 42)

 
(foo
 (let [x 42]
   (reify P
     (foo [this] 17)
     (bar-me [this] x)
     (bar-me [this y] (+ x x)))))

(bar-me
 (let [x 42]
   (reify P
     (foo [this] 17)
     (bar-me [this] x)
     (bar-me [this y] (+ x x)))) 133)

;;for us, this becomes something like


(defprotocol Pp_protocol
  (pp_detail [x])
  (pp_foo [x])
  (pp_bar-me [x] [x y]))

(deftype ParalelPerceptron [a b c]
  Pp_protocol
  (pp_detail [this] (let [z  a 
                     zz c] {:z z :zz zz}))
  (pp_foo [this] a)
  (pp_bar-me [this] b)
  (pp_bar-me [this y] (+ c y)))


(class (pp_foo (ParalelPerceptron. 1 2 3)))
(def my_pp (ParalelPerceptron. 1 2 3))

(pp_detail my_pp)

;; http://pepijndevos.nl/how-reify-works-and-how-to-write-a-custom-typ/index.html

(def myPP (ParalelPerceptron. 1 2 3))



(defrecord pperceptron [ ])
 
