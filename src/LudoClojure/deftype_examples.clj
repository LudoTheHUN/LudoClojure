

;http://www.ibm.com/developerworks/java/library/j-clojure-protocols/index.html#datatypes


(defrecord PurchaseOrder [date customer products]
  Fulfillment
    (invoice [this]
      ... return an invoice based on a PurchaseOrder ... )
    (manifest [this]
      ... return a manifest based on a PurchaseOrder ... ))


(def po (PurchaseOrder. (System/currentTimeMillis)
                        "Mr Foo"
                        ["product1" "product2"]))


;;FAIL  need to figure out defrecord deftype or just build own data structure maps

(defrecord Nuron [nuron_id nuron_type neighbours]
   Connect
    (ConnectSynapse [this]
      (println "takes the neuron and connects it to theres" ))
    (UpdateNeighbours [this]
      ((println "registers this neuron all all neighbours....for each in neighbout add this neuron to thier neighbours list" ))



(defrecord Book [title author])
(defrecord Person [first-name last-name])

(def lotr (Book. "Book Title"
                       (Person. "first" "last")))

(def lotr2 (Book. "Book Title"
                       "fooperson"))

(-> lotr :author :first-name)
(:first-name (:author lotr))


(def Tolkins (Person. "Tol1" "Tol2"))

(def lotr3 (Book. "lot3book title" Tolkins))

(defrecord BookShelf [arrayOfBooks])
(def MyBookShelf (BookShelf. [lotr lotr2 lotr3]))
;;; What would a 'method' that gets all authors on this bookshelf look like??

;;TODO figure out how to add a 'method' function (if that's the right terminology) that is associated with instances of a defrecord...

(def lotr3 (Book. "lot3book3 title" Tolkins))

;;;;;;;;;;;;;;;




















