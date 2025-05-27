import Debug "mo:base/Debug";
import Principal "mo:base/Principal";
import Text "mo:base/Text";
import Array "mo:base/Array";
import Time "mo:base/Time";
import Int "mo:base/Int";

actor {
  type HrvEntry = {
    timestamp: Int;
    rmssd: Nat;
    surroundings: Nat;
    self: Nat;
    interaction: Nat;
    place: Nat;
    technology: Nat;
    stress: Text;
  };
  
  type User = {
    principal: Principal;
    group: Text;
    points: Nat;
    hrv: [HrvEntry];
  };
  
  private stable var users: [User] = [];
  private stable var owner: Principal = Principal.fromText("2vxsx-fae");
  
  public shared(msg) func register(group: Text): async Bool {
    let caller = msg.caller;
    for (user in users.vals()) {
      if (user.principal == caller) {
        return false;
      };
    };
    users := Array.append(users, [{
      principal = caller;
      group = group;
      points = 0;
      hrv = []
    }]);
    Debug.print("Nuevo usuario: " # Principal.toText(caller));
    return true;
  };
  
  private func calculateStressScore(rmssd: Nat): Text {
    if (rmssd > 50) {
      return "Low Stress";
    } else if (rmssd >= 30 and rmssd <= 50) {
      return "Medium Stress";
    } else {
      return "High Stress";
    };
  };
  
  public shared(msg) func addHrv(
    rmssd: Nat,
    surroundings: Nat,
    self: Nat,
    interaction: Nat,
    place: Nat,
    technology: Nat
  ): async Bool {
    let principal = msg.caller;
    var index: ?Nat = null;
    
    for (i in users.keys()) {
      if (users[i].principal == principal) {
        index := ?i;
      };
    };
    
    switch (index) {
      case null {
        Debug.print("Usuario no registrado: " # Principal.toText(principal));
        return false;
      };
      case (?i) {
        let level = calculateStressScore(rmssd);
        var prev: ?Text = null;
        if (users[i].hrv.size() > 0) {
          let last = users[i].hrv[users[i].hrv.size() - 1];
          prev := ?last.stress;
        };
        
        var earned: Nat = 0;
        switch (prev) {
          case (?prevLevel) {
            if (prevLevel == "High Stress" and (level == "Medium Stress" or level == "Low Stress")) {
              earned := 10;
            } else if (prevLevel == "Medium Stress" and level == "Low Stress") {
              earned := 5;
            };
          };
          case null {};
        };
        
        let newUser = {
          principal = users[i].principal;
          group = users[i].group;
          points = users[i].points + earned;
          hrv = Array.append(users[i].hrv, [{
            timestamp = Time.now();
            rmssd = rmssd;
            surroundings = surroundings;
            self = self;
            interaction = interaction;
            place = place;
            technology = technology;
            stress = level
          }]);
        };
        
        users := Array.tabulate<User>(users.size(), func(j) {
          if (j == i) { newUser } else { users[j] }
        });
        
        Debug.print("Puntos ganados: " # debug_show(earned));
        return true;
      };
    };
  };
  
  public shared query(msg) func getUser(): async ?User {
    let caller = msg.caller;
    for (user in users.vals()) {
      if (user.principal == caller) {
        return ?user;
      };
    };
    return null;
  };
  
  public shared query(msg) func getPoints(): async ?Nat {
    let caller = msg.caller;
    for (user in users.vals()) {
      if (user.principal == caller) {
        return ?user.points;
      };
    };
    return null;
  };
  
  public query func feedback(level: Text): async Text {
    switch (level) {
      case "Low Stress" {
        return "Excelente. Sigue así.";
      };
      case "Medium Stress" {
        return "Puedes mejorar. Prueba meditación.";
      };
      case "High Stress" {
        return "Alerta. Busca ayuda o respira profundo.";
      };
      case _ {
        return "No se puede dar feedback.";
      };
    };
  };
  
  public query func getOwner(): async Principal {
    return owner;
  };
  
  public shared(msg) func isOwner(): async Bool {
    return msg.caller == owner;
  };
}