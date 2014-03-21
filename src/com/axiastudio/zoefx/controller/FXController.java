package com.axiastudio.zoefx.controller;

import com.axiastudio.zoefx.db.Model;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * User: tiziano
 * Date: 20/03/14
 * Time: 23:04
 */
public class FXController implements Initializable {

    private Scene scene;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void setScene(Scene scene){
        this.scene = scene;
    }

    public void bindModel(Model model){
        Parent root = this.scene.getRoot();
        AnchorPane pane = (AnchorPane) root;
        for( Node node: pane.getChildren() ){
            String name = node.getId();
            Property property = model.getProperty(name);
            if( node instanceof TextField ){
                Bindings.bindBidirectional(((TextField) node).textProperty(), property);
            } else if( node instanceof TextArea){
                Bindings.bindBidirectional(((TextArea) node).textProperty(), property);
            }
        }
    }

}