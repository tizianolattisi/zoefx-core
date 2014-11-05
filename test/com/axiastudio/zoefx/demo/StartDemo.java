package com.axiastudio.zoefx.demo;

import com.axiastudio.zoefx.core.Utilities;
import com.axiastudio.zoefx.core.model.beans.EntityBuilder;
import com.axiastudio.zoefx.core.controller.Controller;
import com.axiastudio.zoefx.core.db.Database;
import com.axiastudio.zoefx.core.db.NoPersistenceDatabaseImpl;
import com.axiastudio.zoefx.core.skins.*;
import com.axiastudio.zoefx.core.validators.ValidatorBuilder;
import com.axiastudio.zoefx.core.validators.Validators;
import com.axiastudio.zoefx.core.view.SceneBuilders;
import com.axiastudio.zoefx.core.view.ZSceneBuilder;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * User: tiziano
 * Date: 18/03/14
 * Time: 20:38
 */
public class StartDemo extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        //ZSkin skin = new Black();
        ZSkin skin = new FamFamFam();
        //ZSkin skin = new NoIcons();
        Skins.registerSkin(skin);

        NoPersistenceDatabaseImpl database = new NoPersistenceDatabaseImpl();
        Utilities.registerUtility(database, Database.class);

        initData();

        Validators.bindValidator(Book.class, "title", ValidatorBuilder.create().minLength(2).maxLength(5).build());

        ZSceneBuilder zsbBook = ZSceneBuilder.create()
                .url(StartDemo.class.getResource("/com/axiastudio/zoefx/demo/books.fxml"))
                .controller(new Controller())
                .manager(database.createManager(Book.class));
        zsbBook = zsbBook.properties(StartDemo.class.getResource("/com/axiastudio/zoefx/demo/book.properties"));
        SceneBuilders.registerSceneBuilder(Book.class, zsbBook);

        ZSceneBuilder zsbPerson = ZSceneBuilder.create().url(StartDemo.class.getResource("/com/axiastudio/zoefx/demo/persons.fxml"))
                .properties(StartDemo.class.getResource("/com/axiastudio/zoefx/demo/person.properties"))
                .controller(new Controller()).manager(database.createManager(Person.class));
        SceneBuilders.registerSceneBuilder(Person.class, zsbPerson);


        ZSceneBuilder zsbAuthor = ZSceneBuilder.create().url(StartDemo.class.getResource("/com/axiastudio/zoefx/demo/authors.fxml"))
                .controller(new Controller()).manager(database.createManager(Author.class));
        SceneBuilders.registerSceneBuilder(Author.class, zsbAuthor);

        primaryStage.setTitle("Zoe FX Framework - Books");
        primaryStage.setScene(zsbBook.build().getScene());
        primaryStage.show();


        Stage authorStage = new Stage();
        authorStage.setTitle("Zoe FX Framework - Authors");
        authorStage.setScene(zsbAuthor.build().getScene());
        authorStage.show();

        Stage personStage = new Stage();
        personStage.setTitle("Zoe FX Framework - Loans");
        personStage.setScene(zsbPerson.build().getScene());
        personStage.show();


    }

    public static void main(String[] args){
        Application.launch(StartDemo.class, args);
    }

    private static void initData(){

        NoPersistenceDatabaseImpl database = (NoPersistenceDatabaseImpl) Utilities.queryUtility(Database.class);


        Author lev = EntityBuilder.create(Author.class).set("name", "Lev").set("surname", "Tolstoj").build();
        Author marquez = EntityBuilder.create(Author.class).set("name", "Gabriel García").set("surname", "Márquez").build();

        Book karenina = EntityBuilder.create(Book.class).set("title", "Anna Karenina").set("year", 2000).set("finished", true)
                .set("description", "A very long book...").set("genre", Genre.ROMANCE).set("author", lev).build();

        Book wnp = EntityBuilder.create(Book.class).set("title", "War and peace").set("year", 2000).set("finished", false)
                .set("description", "Another long book...").set("genre", Genre.HISTORIC).set("author", lev).build();

        Book yos = EntityBuilder.create(Book.class).set("title", "100 years of solitude").set("year", 2000).set("finished", false)
                .set("description", "A beautiful book.").set("genre", Genre.ROMANCE).set("author", marquez).build();

        // beacause we don't have a db...
        lev.books.add(karenina);
        lev.books.add(wnp);
        marquez.books.add(yos);

        List<Book> books = new ArrayList<Book>();
        books.add(karenina);
        books.add(wnp);
        books.add(yos);
        database.putStore(books, Book.class);

        List<Author> authors = new ArrayList<>();
        authors.add(lev);
        authors.add(marquez);
        database.putStore(authors, Author.class);

        Person tiziano = EntityBuilder.create(Person.class).set("name", "Tiziano").set("surname", "Lattisi").build();
        Loan loan = EntityBuilder.create(Loan.class).set("book", karenina).set("person", tiziano)
                .set("note", "To return- ;-)").build();
        tiziano.loans.add(loan);

        List<Loan> loans = new ArrayList<>();
        loans.add(loan);
        database.putStore(loans, Loan.class);

        List<Person> persons = new ArrayList<>();
        persons.add(tiziano);
        database.putStore(persons, Person.class);

    }

}
