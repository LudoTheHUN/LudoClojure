

(ns LudoClojure.core
  (:require clojure.contrib.seq-utils)
  (:require clojure.contrib.probabilities.random-numbers)  ;;gives lcg random stream generator
  (:require clojure.contrib.duck-streams )
  (:gen-class))

;Java interop examples
(import '(java.awt AWTException Robot Rectangle Toolkit)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D)
        )



;;Serialization to disk
;;;http://richhickey.github.com/clojure-contrib/duck-streams-api.html      
;;Very bad that I had to add these dependencies and paths within these functons
;;clojure.contrib.duck-streams/with-out-writer  
;;'(java.io File IOException PushbackReader FileReader)
(defn serialize
  "Print a data structure to a file so that we may read it in later."
  [data-structure #^String filename]
  (clojure.contrib.duck-streams/with-out-writer
    (java.io.File. filename)
    (binding [*print-dup* true] (prn data-structure))))

;; This allows us to then read in the structure at a later time, like so:
(defn deserialize [filename]
  (with-open [r (PushbackReader. (FileReader. filename))]
    (read r)))

;Testing Serialization and De-Serialization
(serialize '(def y 50)  "/home/ludo/Documents/serializationTest2")
(deserialize "/home/ludo/Documents/serializationTest")

(def boo (myCLJarrayFromJavaArrayFun  (GrabScreenColorJavaArray  0 0 x y)))
(serialize boo "/home/ludo/Documents/serializationTest2")
(def boo2 (deserialize "/home/ludo/Documents/serializationTest2"))

