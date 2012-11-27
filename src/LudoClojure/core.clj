(ns LudoClojure.core
 ; (:use LudoClojure.Win_scrachAlwaysRun)
  ;(:require calx)  ;;this means that I don't haveto have a copy of the calx source in src
  (:use [LudoClojure.pperceptron]
        [LudoClojure.opencl-utils])
  ;(:require clojure.contrib.duck-streams )   ;needed for serde
  ;(:require clojure.contrib.seq-utils)       ;needed for serde
  ;(:require [LudoClojure.randomnumberexplorer.randomnumerexplorer :as UI1])
  ;(:require [LudoClojure.timelooper1.timelooper1 :as UI2])

  (:gen-class))
  
(import '(java.awt AWTException Robot Rectangle Toolkit GridLayout GridBagLayout GridBagConstraints Insets)
        '(java.io File IOException PushbackReader FileReader)
        '(java.awt.Robot.)
        '(java.awt.image BufferedImage DirectColorModel PixelGrabber)
        '(javax.imageio ImageIO)
        '(java.awt Color Graphics Graphics2D Dimension)
        '(java.awt.event.ActionListener)
        '(javax.swing JPanel JFrame JSlider BoxLayout JLabel JButton SwingConstants)
        '(javax.swing.event ChangeListener ChangeEvent)
        )

;(:import [com.nativelibs4java.opencl CLContext CLByteBuffer CLMem CLMem$Usage CLEvent]
;         [com.nativelibs4java.util NIOUtils]
;        [java.nio ByteOrder ByteBuffer])


;(use 'calx)
;(use 'clojure.contrib.math)

(set! *warn-on-reflection* false)






(defn hello
  ([] "Hello world!")
  ([name] (str "Hello " name "!")))


(defn -main [& args]
  (println "Please run with options:
           1: Random number generator with data sourced from openCL
           2: Funk renderer, leading to neuron readout scaner...")
  (println (hello args))
  (when (=  (str (first args)) "1") (do 
                                    (println "option1 selcted")
                                    ;(UI1/frame)
                                    ))
  (when (=  (str (first args)) "2") (do 
                                    (println "option2 selcted")
                                    ;(UI2/frame)
									))
  (when (= (str(first args)) nil) (do 
                                    (println "no options"))))


(println "hello there from core.clj file, we are running on "
    "\n version:                 "(System/getProperty "java.version")
    "\n vm.version:              " (System/getProperty "java.vm.version")
    "\n vm.specification.version:" (System/getProperty "java.vm.specification.version")
                                       )

;(use 'LudoClojure.randomnumberexplorer.randomnumerexplorer)

(quote Key	Description of Associated Value
(System/getProperty "java.version")
java.vendor
java.vendor.url
java.home
java.vm.specification.version
java.vm.specification.vendor
java.vm.specification.name
java.vm.version
java.vm.vendor
java.vm.name
java.specification.version
java.specification.vendor
java.specification.name
java.class.version
java.class.path
java.library.path
java.io.tmpdir
java.compiler
java.ext.dirs
os.name
os.arch
os.version
file.separator
path.separator
line.separator
user.name
user.home
user.dir)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Iteration loops33
;TODO : Super clean refactor


;TODO close over the opencl code... or rather send it off onto another thread.?
;Note innner loop within openCL context can not be quit without loose hooks to openCL buffers??
;Can we onload and offload within this loop when it is setoff on another thread?
;To get the image to refresh , just need to close over (closure style) openCL code , passing in the veriables?? But longer term, the opencl should be on a separate thread, in ... have a listener ...look into sendof?



;TODO animate over growing moding values for random number geerator..., do it with slider...
;TODO Add a user interface, slider?, for the moding value.
     ;TODO DONE set a lay out strategy... DONE!
     ;TODO DONEconnect up action listeners
     ;TODO put openCL on a action listener dependet loop (events)	
;TODO recode GUI with ???  http://lifeofaprogrammergeek.blogspot.com/2009/05/model-view-controller-gui-in-clojure.html
;look at: http://kotka.de/blog/2010/03/proxy_gen-class_little_brother.html
;  http://stuartsierra.com/2010/01/05/taming-the-gridbaglayout


;(in-ns 'LudoClojure.timelooper1.timelooper1)



