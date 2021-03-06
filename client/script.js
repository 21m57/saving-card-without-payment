var stripeElements = function(publicKey, setupIntent) {
  var stripe = Stripe(publicKey);
  var elements = stripe.elements();

  // Element styles
  var style = {
    base: {
      fontSize: "16px",
      color: "#32325d",
      fontFamily:
        "-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif",
      fontSmoothing: "antialiased",
      "::placeholder": {
        color: "rgba(0,0,0,0.4)"
      }
    }
  };

  var card = elements.create("card", { style: style });

  card.mount("#card-element");

  // Element focus ring
  card.on("focus", function() {
    var el = document.getElementById("card-element");
    el.classList.add("focused");
  });

  card.on("blur", function() {
    var el = document.getElementById("card-element");
    el.classList.remove("focused");
  });

  // Handle payment submission when user clicks the pay button.
  var form = document.getElementById("payment-form");
  form.addEventListener("submit", function(event) {
    event.preventDefault();
    changeLoadingState(true);
    stripe
      .handleCardSetup(setupIntent.client_secret, card)
      .then(function(result) {
        if (result.error) {
          changeLoadingState(false);
          var displayError = document.getElementById("card-errors");
          displayError.textContent = result.error.message;
        } else {
          fetch("/create-customer", {
            method: "post",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify(result.setupIntent)
          })
            .then(function(response) {
              return response.json();
            })
            .then(function(customer) {
              changeLoadingState(false);
              document.getElementById("endstate").style.display = "block";
              document.getElementById("startstate").style.display = "none";
              document.getElementById(
                "setupIntent-response"
              ).innerHTML = JSON.stringify(result.setupIntent, null, 2);
            });
        }
      });
  });
};

function getSetupIntent(publicKey) {
  return fetch("/create-setup-intent", {
    method: "post",
    headers: {
      "Content-Type": "application/json"
    }
  })
    .then(function(response) {
      return response.json();
    })
    .then(function(setupIntent) {
      stripeElements(publicKey, setupIntent);
    });
}

function getPublicKey() {
  return fetch("/public-key", {
    method: "get",
    headers: {
      "Content-Type": "application/json"
    }
  })
    .then(function(response) {
      return response.json();
    })
    .then(function(response) {
      getSetupIntent(response.publicKey);
    });
}

// Show a spinner on payment submission
function changeLoadingState(isLoading) {
  if (isLoading) {
    document.querySelector("button").disabled = true;
    document.querySelector("#spinner").classList.remove("hidden");
    document.querySelector("#button-text").classList.add("hidden");
  } else {
    document.querySelector("button").disabled = false;
    document.querySelector("#spinner").classList.add("hidden");
    document.querySelector("#button-text").classList.remove("hidden");
  }
}

getPublicKey();
