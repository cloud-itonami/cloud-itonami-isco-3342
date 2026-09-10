(ns legalsecretary.store
  "SSoT for the ISCO-08 3342 independent legal secretarial practice
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a document intake and
  case-file coordination robot performs filing, docketing and
  physical archival under this advisor/governor pair, which never
  dispatches hardware itself and never finalizes disclosure of
  privileged/confidential case information, provides legal advice, or
  sets a filing deadline/legal strategy without attorney sign-off).
  Modeled on cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    attorney — a registered supervising attorney
               (:attorney-id, :name)
    case     — a registered case file {:case-id :attorney-id :name
               :max-supply-cost number}. `:max-supply-cost` is the
               registered ceiling above which a coordinate-supply-order
               proposal must escalate to human sign-off rather than
               auto-commit — office/case-material procurement above
               the registered threshold is not routine secretarial
               work.
    record   — a committed operating record (a logged document, a
               scheduled court operation, a flagged confidentiality
               concern, or a coordinated supply order) — written ONLY
               via commit-record!.
    ledger   — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (attorney [s attorney-id])
  (case-record [s case-id])
  (records-of [s attorney-id])
  (ledger [s])
  (register-attorney! [s attorney])
  (register-case! [s c])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (attorney [_ attorney-id] (get-in @a [:attorneys attorney-id]))
  (case-record [_ case-id] (get-in @a [:cases case-id]))
  (records-of [_ attorney-id] (filter #(= attorney-id (:attorney-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-attorney! [s att]
    (swap! a assoc-in [:attorneys (:attorney-id att)] att) s)
  (register-case! [s c]
    (swap! a assoc-in [:cases (:case-id c)] c) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:attorneys {} :cases {} :records [] :ledger []}
                                   seed)))))
