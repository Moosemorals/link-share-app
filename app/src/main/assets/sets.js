// From https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set


if (!("difference" in Set.prototype)) {
    Set.prototype.difference = function (setB) {
        let difference = new Set(this);
        for (let elem of setB) {
            difference.delete(elem);
        }
        return difference;
    }
}