package org.example.customers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomerGroupFactory {
    CustomerFactory customerFactory = new CustomerFactory();

    public CustomerGroupFactory() throws IOException {
    }

    public CustomerGroup generateGroup() {
        List<Customer> customerList = new ArrayList<>();

        boolean needsAdultFlag = true;
        do{
            Customer customer = customerFactory.generateCustomer();
            if(customer.isAdult()) needsAdultFlag = false;
            customerList.add(customer);
        }while (needsAdultFlag);

        return new CustomerGroup(customerList);
    }
}
