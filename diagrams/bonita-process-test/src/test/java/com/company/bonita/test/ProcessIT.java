package com.company.bonita.test;

import static com.bonitasoft.test.toolkit.predicate.ProcessInstancePredicates.*;
import static com.bonitasoft.test.toolkit.predicate.UserTaskPredicates.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import com.bonitasoft.test.toolkit.BonitaTestToolkit;
import com.bonitasoft.test.toolkit.contract.ComplexInputBuilder;
import com.bonitasoft.test.toolkit.contract.ContractBuilder;
import com.bonitasoft.test.toolkit.junit.extension.BonitaTests;
import com.bonitasoft.test.toolkit.model.BusinessData;
import com.bonitasoft.test.toolkit.model.BusinessObjectDAO;
import com.bonitasoft.test.toolkit.model.Task;
import com.bonitasoft.test.toolkit.model.QueryParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@BonitaTests 
class CreateDinosaurIT {

    @BeforeEach 
    void beforeEach(BonitaTestToolkit toolkit){
        toolkit.deleteBDMContent(); 
        toolkit.deleteProcessInstances(); 
    }

    @Test
    void should_create_an_hungry_tyrannosaurus(BonitaTestToolkit toolkit) {  
        var user = toolkit.getUser("walter.bates"); 
        var processDef = toolkit.getProcessDefinition("create-dinosaur"); 
        final BusinessObjectDAO<BusinessData> businessObjectDAO = toolkit.getBusinessObjectDAO("com.company.bonitasoft.model.Dinosaur"); 

        assertThat(businessObjectDAO.find(0, 10)).isEmpty();

        var processInstance = processDef.startProcessFor(user); 

        await().until(processInstance, processInstanceStarted()
                .and(containsPendingUserTasks("CreateDinosaur"))); 

        var complexInputBuilder = ComplexInputBuilder.complexInput()
                .textInput("name", "Tyrannosaurus")
                .textInput("color", "Brown")
                .booleanInput("hungry", true);
        var task1Contract = ContractBuilder.newContract().complexInput("dinosaurInput", complexInputBuilder).build(); 
        var task1 = processInstance.getFirstPendingUserTask("CreateDinosaur"); 

        await().until(task1, hasCandidates(user)
                .and(taskReady()));

        task1.execute(user, task1Contract);

        await().until(task1, taskArchived());
        await().until(processInstance, processInstanceArchived());
        assertThat(processInstance.searchTasks()).map(Task::getName).containsExactlyInAnyOrder("CreateDinosaur", "goToHunt");
        assertThat(processInstance.getFirstTask("goToHunt").isArchived()).isTrue();

        // Data assertions
        final List<BusinessData> result = businessObjectDAO.query("findByName",
                                                         List.of(QueryParameter.stringParameter("name", "Tyrannosaurus")), 0, 10);
        assertThat(result)
            .hasSize(1)
            .allSatisfy(dino -> {
                assertThat(dino.getStringField("name")).isEqualTo("Tyrannosaurus");
                assertThat(dino.getStringField("color")).isEqualTo("Brown");
                assertThat(dino.getBooleanField("hungry")).isTrue();
            });
    }

}