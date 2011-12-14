(ns LudoClojure.liquid
  (:require calx)
  (:gen-class))

(use 'calx)
;(use 'clojure.contrib.math)

(set! *warn-on-reflection* false)


(;;??defstruct ....liquid data in out entities?....)

(defn init_liquid! [source_opencl globalsize dataInAtom dataOutAtom]
  "Initialises the liquid"
  (with-cl (def buffer_def_created  (create-buffer (* 16) :int32  ))))

(init_liquid! 1 2 3 4)

 ;; ..
 ;; prcess will consume and produce data,
 ;; how should this be comunicated in and out (atoms)
  )

;;  TODO Q: How to have items within with-cl columicate back to top level... only atoms, do these atoms need to exits before had... do we want an 'object'
(def buffer_atom (atom 0))
(quote 


  
(with-cl
  (swap! buffer_atom  (fn [_] (create-buffer (* 16) :int32  )))
  )


(with-cl
  (def buffer_def  (fn [_] (create-buffer (* 16) :int32  )))
  )

(with-cl
  (def buffer_def_create  (create-buffer (* 16) :int32  ))
  )

(def buffer_def_clean  (create-buffer (* 16) :int32  ))
