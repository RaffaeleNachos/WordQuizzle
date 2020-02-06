![Logo](/Word_Quizzle_Relazione_Apetino/images/quizzlelogolong.png)
## 🖥 Laboratorio di Reti @ Dipartimento di Informatica UniPi 
Il progetto riguarda la programmazione di un sistema di sfide di traduzione (italiano - inglese) utilizzando il linguaggio Java. E' richiesto anche di gestire una rete sociale in cui gli utenti possono registrarsi, aggiungere amicizie e confrontarsi attraverso classifiche basate sul punteggio. La registrazione degli utenti avviene tramite Remote Method Invocation, mentre il dialogo tra client e server secondo connessione TCP (che chiameremo TCP standard). La sfida sfrutta anch'essa il protocollo TCP (TCP sfida). Per le notifiche di sfida tra due utenti è stato richiesto di usare UDP. Le traduzioni si basano sulla API offerta dal servizio gratuito [MyMemory](https://mymemory.translated.net/doc/spec.php).
L'interfaccia grafica è stata realizzata sfruttando il framework [JavaFX](https://docs.oracle.com/javase/8/javafx/get-started-tutorial/jfx-overview.htm) e l'applicativo [SceneBuilder](https://gluonhq.com/products/scene-builder/), che permette la creazione di file FXML attraverso semplici drag and drop, crop e resize dei vari elementi che compongono l'interfaccia. I file FXML in output sono poi caricabili tramite classi apposite di JavaFX come la classe FXMLLoader. Per la gestione dei file di persistenza, contenenti le informazioni delle strutture dati del server, ho utilizzato il formato JSON con l'aiuto delle librerie [JSON.simple](https://code.google.com/archive/p/json-simple/) e [Gson](https://github.com/google/gson/blob/master/UserGuide.md). Per una documentazione più approfondita fare riferimento alla [relazione](https://github.com/RaffaeleNachos/WordQuizzle/blob/master/Word_Quizzle_Relazione_Apetino/Relazione%20Apetino%20549220.pdf).

## 📸 Screenshots
### 🖌 Schermata di registrazione e login
![Login-Register](/Word_Quizzle_Relazione_Apetino/images/quizzleloginregister.png)
### 🔰 Schermata principale
![Login-Register](/Word_Quizzle_Relazione_Apetino/images/quizzlemain.png)
### 🎮 Schermata di gioco
![Login-Register](/Word_Quizzle_Relazione_Apetino/images/quizzlegame.png)

