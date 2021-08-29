package org.gamedo.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.gamedo.persistence.db.ComponentDbData;

@AllArgsConstructor
@Getter
@Setter
public class ComponentDbDataBag extends ComponentDbData {
    int x;
    int y;
}
