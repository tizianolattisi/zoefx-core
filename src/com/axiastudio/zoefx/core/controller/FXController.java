package com.axiastudio.zoefx.core.controller;

import com.axiastudio.zoefx.core.beans.BeanAccess;
import com.axiastudio.zoefx.core.beans.property.ItemObjectProperty;
import com.axiastudio.zoefx.core.events.DataSetEvent;
import com.axiastudio.zoefx.core.events.DataSetEventListener;
import com.axiastudio.zoefx.core.listeners.TextFieldListener;
import com.axiastudio.zoefx.core.validators.Validator;
import com.axiastudio.zoefx.core.validators.Validators;
import com.axiastudio.zoefx.core.db.DataSet;
import com.axiastudio.zoefx.core.view.*;
import com.axiastudio.zoefx.core.console.ConsoleController;
import com.axiastudio.zoefx.core.view.search.SearchController;
import javafx.beans.*;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * User: tiziano
 * Date: 20/03/14
 * Time: 23:04
 */
public class FXController extends BaseController implements DataSetEventListener {

    private Scene scene;
    private DataSet dataset = null;
    private ZSceneMode mode;
    private Behavior behavior = null;
    private FXController me;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        me = this;
    }

    public void setScene(Scene scene){
        this.scene = scene;
    }

    public void bindDataSet(DataSet dataset){
        this.dataset = dataset;
        initializeChoices(); // XXX: INDEX_CHANGED
        initializeColumns(); // XXX: INDEX_CHANGED
        setModel();          // XXX: INDEX_CHANGED
    }


    private void initializeColumns(){
        Model model = dataset.newModel();
        Parent root = this.scene.getRoot();
        Pane container = (Pane) root;
        List<Node> nodes = findNodes(container, new ArrayList<Node>());
        for( Node node: nodes ){
            if( node instanceof TableView){
                TableView tableView = (TableView) node;
                ObservableList<TableColumn> columns = tableView.getColumns();
                for( TableColumn column: columns ){
                    String name = node.getId();
                    //String columnId = column.getId();
                    String columnId = column.getText().toLowerCase(); // XXX: RT-36633 JavaXFX issue
                    // https://javafx-jira.kenai.com/browse/RT-36633
                    String lookup=null;
                    if( behavior != null ) {
                        lookup = behavior.getProperties().getProperty(columnId + ".lookup");
                    }
                    if( lookup != null ) {
                        Callback callback = model.getCallback(name, columnId, lookup);
                        column.setCellValueFactory(callback);
                    } else {
                        Callback callback = model.getCallback(name, columnId);
                        column.setCellValueFactory(callback);
                    }
                    //tableView.getItems().addListener(listChangeListener);
                }
            }
        }
    }

    private void initializeChoices(){
        Model model = dataset.newModel();
        Parent root = this.scene.getRoot();
        Pane container = (Pane) root;
        List<Node> nodes = findNodes(container, new ArrayList<Node>());
        for( Node node: nodes ){
            if( node instanceof ChoiceBox){
                String name = node.getId();
                Property property = model.getProperty(name, Object.class);
                List superset = ((ItemObjectProperty) property).getSuperset();
                ObservableList choices = FXCollections.observableArrayList(superset);
                ((ChoiceBox) node).setItems(choices);
            }
        }
    }

    private void refreshModel() {
        unsetModel();
        setModel();
        //refreshNavBar();
    }

    private void unsetModel() {
        configureModel(false);
    }

    private void setModel() {
        configureModel(true);
    }

    private void configureModel(Boolean isSet) {
        Model model;
        if( isSet ) {
            model = dataset.newModel();
        } else {
            model = dataset.getCurrentModel();
        }
        Parent root = scene.getRoot();
        Pane container = (Pane) root;
        List<Node> nodes = findNodes(container, new ArrayList<Node>());
        for( Node node: nodes ){
            String name = node.getId();
            Property leftProperty = null;
            Property rightProperty = null;
            if( node instanceof TextField ){
                leftProperty = ((TextField) node).textProperty();
                rightProperty = model.getProperty(name, String.class);
                Validator validator = Validators.getValidator(model.getEntityClass(), name);
                if( validator != null ) {
                    leftProperty.addListener(new TextFieldListener(validator));
                }
            } else if( node instanceof TextArea ){
                leftProperty = ((TextArea) node).textProperty();
                rightProperty = model.getProperty(name, String.class);
            } else if( node instanceof CheckBox ){
                leftProperty = ((CheckBox) node).selectedProperty();
                rightProperty = model.getProperty(name, Boolean.class);
            } else if( node instanceof ChoiceBox ){
                leftProperty = ((ChoiceBox) node).valueProperty();
                rightProperty = model.getProperty(name, Object.class);
            } else if( node instanceof DatePicker ){
                leftProperty = ((DatePicker) node).valueProperty();
                rightProperty = model.getProperty(name, Date.class);
            } else if( node instanceof TableView ){
                TableView tableView = (TableView) node;
                tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                tableView.setContextMenu(createContextMenu(tableView));
                leftProperty = tableView.itemsProperty();
                rightProperty = model.getProperty(name, Collection.class);
            }
            if( rightProperty != null && leftProperty != null) {
                if( isSet ) {
                    Bindings.bindBidirectional(leftProperty, rightProperty);
                    leftProperty.addListener(invalidationListener);
                } else {
                    Bindings.unbindBidirectional(leftProperty, rightProperty);
                    //rightProperty.unbind();
                    leftProperty.removeListener(invalidationListener);
                }
                dataset.putOldValue(leftProperty, leftProperty.getValue());
            }
        }
    }

    private ContextMenu createContextMenu(TableView tableView){
        ContextMenu contextMenu = new ContextMenu();

        MenuItem infoItem = new MenuItem("Information");
        infoItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/info.png"))));
        infoItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                ObservableList selectedItems = tableView.getSelectionModel().getSelectedItems();
                if( selectedItems.size()==0 ) {
                    return;
                }
                List<Object> newStore = new ArrayList<>();
                for( int i=0; i<selectedItems.size(); i++ ) {
                    newStore.add(selectedItems.get(i));
                }
                ZScene newScene = SceneBuilders.queryZScene(newStore, ZSceneMode.DIALOG);
                if( newScene != null ) {
                    Stage newStage = new Stage();
                    newStage.setScene(newScene.getScene());
                    newStage.show();
                }
            }
        });
        MenuItem openItem = new MenuItem("Open");
        openItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/open.png"))));
                openItem.setOnAction(new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent e) {
                        ObservableList selectedItems = tableView.getSelectionModel().getSelectedItems();
                        if (selectedItems.size() == 0) {
                            return;
                        }
                        List<Object> newStore = new ArrayList<>();
                        String referenceProperty = tableView.getId() + ".reference";
                        String reference = behavior.getProperties().getProperty(referenceProperty, null);
                        for (int i = 0; i < selectedItems.size(); i++) {
                            Object item = selectedItems.get(i);
                            if (reference != null) {
                                BeanAccess<Object> ba = new BeanAccess<>(item, reference);
                                newStore.add(ba.getValue());
                            } else {
                                newStore.add(item);
                            }
                        }
                        ZScene newScene = SceneBuilders.queryZScene(newStore, ZSceneMode.DIALOG);
                        if (newScene != null) {
                            Stage newStage = new Stage();
                            newStage.setScene(newScene.getScene());
                            newStage.show();
                        }
                    }
                });
        MenuItem addItem = new MenuItem("Add");
        addItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/add.png"))));
        addItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                String referenceProperty = tableView.getId() + ".reference";
                String reference = behavior.getProperties().getProperty(referenceProperty, null);
                if (reference != null) {
                    System.out.println("Search and select " + referenceProperty);
                    dataset.create(tableView.getId());
                } else {
                    dataset.create(tableView.getId());
                }
                refreshModel();
                dataset.getDirty();
            }
        });
        MenuItem delItem = new MenuItem("Delete");
        delItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/delete.png"))));
        delItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                System.out.println("Delete!");
            }
        });

        contextMenu.getItems().addAll(infoItem, openItem, addItem, delItem);
        return contextMenu;
    }



    private List<Node> findNodes( Pane container, List<Node> nodes ){
        for( Node node: container.getChildren() ){
            if( node instanceof Pane ){
                nodes = findNodes((Pane) node, nodes);
            } else if( node instanceof TabPane ){
                for( Tab tab: ((TabPane) node).getTabs() ) {
                    nodes = findNodes((Pane) tab.getContent(), nodes);
                }
            }
            else if( node.getId() != null && node.getId() != "" ){
                nodes.add(node);
            }
        }
        return nodes;
    }


    public DataSet getDataset() {
        return dataset;
    }

    private FXController self(){
        return this;
    }

    public ZSceneMode getMode() {
        return mode;
    }

    public void setMode(ZSceneMode mode) {
        this.mode = mode;
    }

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    /*
    public void refresh(){
        unsetModel();
        dataset.goFirst();
        setModel();
        //refreshNavBar();
    }*/

    /*
     *  Navigation Bar
     */

    public EventHandler<ActionEvent> handlerGoFirst = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goFirst();
            setModel();
        }
    };
    public EventHandler<ActionEvent> handlerGoPrevious = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goPrevious();
            setModel();
        }
    };
    public EventHandler<ActionEvent> handlerGoNext = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goNext();
            setModel();
        }
    };
    public EventHandler<ActionEvent> handlerGoLast = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goLast();
            setModel();
        }
    };
    public EventHandler<ActionEvent> handlerSave = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            dataset.commit();
        }
    };
    public EventHandler<ActionEvent> handlerConfirm = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            ((Stage) scene.getWindow()).close();
            // TODO: get the parent dirty
        }
    };
    public EventHandler<ActionEvent> handlerCancel = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            dataset.revert();
        }
    };
    public EventHandler<ActionEvent> handlerAdd = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            dataset.create();
            refreshModel();
        }
    };
    public EventHandler<ActionEvent> handlerSearch = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            URL url = getClass().getResource("/com/axiastudio/zoefx/core/view/search/search.fxml");
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(url);
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            Parent root = null;
            try {
                root = loader.load(url.openStream());
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            SearchController controller = loader.getController();
            controller.setEntityClass(dataset.getCurrentModel().getEntityClass());
            List<String> columns = new ArrayList<>();
            String searchcolumns = behavior.getProperties().getProperty("searchcolumns");
            if( searchcolumns != null ){
                String[] split = searchcolumns.split(",");
                for( int i=0; i<split.length; i++ ){
                    columns.add(split[i]);
                }
            }
            controller.setColumns(columns);
            controller.setParentController(me);

            Stage stage = new Stage();
            stage.setTitle("Search");
            stage.setScene(new Scene(root, 450, 450));
            stage.show();
        }
    };
    public EventHandler<ActionEvent> handlerDelete = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            dataset.delete();
        }
    };
    public EventHandler<ActionEvent> handlerConsole = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            URL url = getClass().getResource("/com/axiastudio/zoefx/core/console/console.fxml");
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(url);
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            Parent root = null;
            try {
                root = loader.load(url.openStream());
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            ConsoleController console = loader.getController();
            console.setController(self());

            Stage stage = new Stage();
            stage.setTitle("Zoe FX Script Console");
            stage.setScene(new Scene(root, 450, 450));
            stage.show();
        }
    };


    /*
     *  Listeners
     */

    public InvalidationListener invalidationListener = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            dataset.getDirty();
        }
    };


    @Override
    public void dataSetEventHandler(DataSetEvent event) {
        System.out.println(event.getEventType() + " -> controller");
        if( event.getEventType().equals(DataSetEvent.STORE_CHANGED) ){
            refreshModel();
        } else if( event.getEventType().equals(DataSetEvent.REVERT) ){
            refreshModel();
        }
    }
}