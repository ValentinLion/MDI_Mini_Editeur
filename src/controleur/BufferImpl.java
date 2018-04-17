package controleur;

import commands.Coller;
import commands.Command;
import commands.Copier;
import commands.Couper;
import commands.Inserer;
import commands.Supprimer;
import enregistreur.EnregistreurMacro;
import enregistreur.EnregistreurUndoRedo;
import enregistreur.EnregistreurUndoRedoImpl;
import enregistreur.ListEnregistrementMacro;
import enregistreur.ListEnregistrementMacroImpl;
import enregistreur.MementoCommande;
import vue.IHM;

public class BufferImpl implements Buffer {

	private PressePapier pressePapier = new PressePapierImpl();
	private Selecteur selecteur = new SelecteurImpl(this);
	private IHM ihm;

	// Texte du buffer
	private String content = "";

	// Sauvegarde des macros
	private ListEnregistrementMacro listMacro = new ListEnregistrementMacroImpl();

	// Sauvegarde des Undo/Redo
	private EnregistreurUndoRedo enregistreurUndoRedo = new EnregistreurUndoRedoImpl();

	// Memento en cours d'exécution
	private MementoCommande enregistrementCourant;

	// Flag indiquant si la la commande en cours est rejoué
	private boolean rejoue = false;

	private int selectedMacro = -1;

	public BufferImpl(IHM ihm) {
		this.ihm = ihm;
	}

	// Fonction appelées par les commandes

	public void copier() {

		pressePapier.ecrire(selecteur.lire());

		if (listMacro.isRecording()) {
			listMacro.addEnregistrement(createMemento(new Copier(this), ""));
		}

		// enregistreurUndoRedo.addCommand(createMemento(new
		// Copier(this),""),!rejoue);

	}

	public void couper() {

		System.out.println("couper " + selecteur.lire());

		String deletedCharacters = selecteur.lire();

		pressePapier.ecrire(selecteur.lire());
		content = content.substring(0, selecteur.getDebut()) + content.substring(selecteur.getFin(), content.length());

		if (listMacro.isRecording()) {
			listMacro.addEnregistrement(createMemento(new Couper(this), deletedCharacters));
		}

		enregistreurUndoRedo.addCommand(createMemento(new Couper(this), deletedCharacters), !rejoue);

		int newCursorPosition = selecteur.getDebut();
		ihm.update();
		ihm.setCursorPosition(newCursorPosition);
	}

	public void coller() {

		System.out.println("coller " + pressePapier.lire());

		int newCursorPosition;

		if (rejoue) {
			newCursorPosition = selecteur.getFin() + enregistrementCourant.getText().length();
			content = content.substring(0, selecteur.getDebut()) + enregistrementCourant.getText()
					+ content.substring(selecteur.getDebut(), content.length());
		} else {
			newCursorPosition = selecteur.getFin() + pressePapier.lire().length();
			content = content.substring(0, selecteur.getDebut()) + pressePapier.lire()
					+ content.substring(selecteur.getDebut(), content.length());
		}

		if (listMacro.isRecording()) {
			listMacro.addEnregistrement(createMemento(new Coller(this), pressePapier.lire()));
		}

		enregistreurUndoRedo.addCommand(createMemento(new Coller(this), pressePapier.lire()), !rejoue);

		ihm.update();
		ihm.setCursorPosition(newCursorPosition);

	}

	public void inserer() {

		int position = selecteur.getDebut();
		String character = ihm.getCaractereInsere();

		if (rejoue) {
			position = enregistrementCourant.getDebutSelection();
			character = enregistrementCourant.getText();
		}

		System.out.println("Insert " + character + " at " + position + " content = " + content);
		content = new StringBuilder(content).insert(position, character).toString();

		// Si on enregistre une macro -> on l'ajoute à l'enregistrement
		if (listMacro.isRecording()) {
			listMacro.addEnregistrement(createMemento(new Inserer(this), character));
		}

		// On l'ajoute à l'historique des commandes
		enregistreurUndoRedo.addCommand(createMemento(new Inserer(this), character), !rejoue);

		int newCursorPosition = selecteur.getFin() + 1;
		ihm.update();
		ihm.setCursorPosition(newCursorPosition);

	}

	public void selection() {
		selecteur.setDebut(ihm.getDebutSelection());
		selecteur.setFin(ihm.getFinSelection());
	}

	/**
	 * Rejoue la macro selectionnée
	 */
	public void rejouer() {
		listMacro.getMacro(selectedMacro).rejouer(this);
	}

	/**
	 * Retourne le texte du buffer
	 * 
	 * @return
	 */
	public String getText() {
		return content;
	}

	// Fonction utiles pour les macros

	/**
	 * Créer un memento de la commande
	 */
	public MementoCommande createMemento(Command commande, String text) {
		MementoCommande enr = new MementoCommande();
		enr.setDebutSelection(selecteur.getDebut());
		enr.setFinSelection(selecteur.getFin());
		enr.setCommande(commande);
		enr.setText(text);
		return enr;
	}

	/**
	 * Exécute la commande contenue dans le memento
	 * 
	 * @param enr
	 */
	public void setMemento(MementoCommande enr) {

		rejoue = true;

		this.enregistrementCourant = enr;
		selecteur.setDebut(enregistrementCourant.getDebutSelection());
		selecteur.setFin(enregistrementCourant.getFinSelection());
		enregistrementCourant.getCommande().exec();

		rejoue = false;
	}

	// Fonctions utiles pour les Undo/Redo

	/**
	 * Annule la dernière action
	 */
	public void undo() {

		if (enregistreurUndoRedo.UndoPossible()) {

			enregistrementCourant = enregistreurUndoRedo.getUndo();

			selecteur.setDebut(enregistrementCourant.getDebutSelection());
			selecteur.setFin(enregistrementCourant.getFinSelection());
			enregistrementCourant.getCommande().unexec();
		}
	}

	/**
	 * Rejoue la dernière action
	 */
	public void redo() {

		if (enregistreurUndoRedo.RedoPossible()) {
			enregistrementCourant = enregistreurUndoRedo.getRedo();

			selecteur.setDebut(enregistrementCourant.getDebutSelection());
			selecteur.setFin(enregistrementCourant.getFinSelection());
			rejoue = true;
			enregistrementCourant.getCommande().exec();
			rejoue = false;
		}
	}

	public void decoller() {
		content = content.substring(0, enregistrementCourant.getDebutSelection()) + content.substring(
				enregistrementCourant.getDebutSelection() + enregistrementCourant.getText().length(), content.length());
		int newCursorPosition = enregistrementCourant.getFinSelection();
		ihm.update();
		ihm.setCursorPosition(newCursorPosition);
	}

	public void decouper() {
		content = content.substring(0, enregistrementCourant.getDebutSelection()) + enregistrementCourant.getText()
				+ content.substring(enregistrementCourant.getDebutSelection(), content.length());
		int newCursorPosition = enregistrementCourant.getDebutSelection() + enregistrementCourant.getText().length();
		ihm.update();
		ihm.setCursorPosition(newCursorPosition);
	}

	public void deinserer() {

		content = content.substring(0, enregistrementCourant.getDebutSelection())
				+ content.substring(enregistrementCourant.getDebutSelection() + 1, content.length());
		int newCursorPosition = enregistrementCourant.getDebutSelection();
		ihm.update();
		ihm.setCursorPosition(newCursorPosition);

	}

	public void decopier() {

	}

	// Fonctions utilitaire

	private String removeCharacters(int start, int end) {
		System.out.println("suppression de " + start + " à " + end);

		String deletedCharacters = content.substring(start, end);

		content = content.substring(0, start) + content.substring(end, content.length());

		return deletedCharacters;
	}

	public EnregistreurMacro newMacro() {
		return listMacro.newMacro();
	}

	public ListEnregistrementMacro getListEnregistreurMacro() {
		return this.listMacro;
	}

	public void supprimer() {

		int startPosition = selecteur.getDebut();
		int endPosition = selecteur.getFin();

		if (startPosition > 0) {
			if (rejoue) {
				startPosition = enregistrementCourant.getDebutSelection();
				endPosition = enregistrementCourant.getFinSelection();
			}

			if (startPosition == endPosition) {
				startPosition--;
			}
			String deletedCharacters = removeCharacters(startPosition, endPosition);

			// Si on enregistre une macro -> on l'ajoute à l'enregistrement
			if (listMacro.isRecording()) {
				listMacro.addEnregistrement(createMemento(new Supprimer(this), deletedCharacters));
			}

			// On l'ajoute à l'historique des commandes
			enregistreurUndoRedo.addCommand(createMemento(new Supprimer(this), deletedCharacters), !rejoue);

			ihm.update();
			ihm.setCursorPosition(startPosition);
		}
	}

	public void desupprimer() {

		int positionStart = enregistrementCourant.getDebutSelection();
		int positionEnd = enregistrementCourant.getFinSelection();

		if (positionEnd == positionStart) {
			positionStart--;
		}

		String character = enregistrementCourant.getText();

		content = new StringBuilder(content).insert(positionStart, character).toString();

		ihm.update();
		ihm.setCursorPosition(positionEnd);
	}

	public void setSelectedMacro(int selectedIndex) {
		this.selectedMacro = selectedIndex;
	}

}