import './styles.scss'

// SockJS patch
window.global = window;

document.querySelector<HTMLDivElement>('#app')!.innerHTML = `
  <img class="rain-svg" src="/rain-stripes.svg" alt="background-pattern">
  
  <div class="main-container">
    <div class="search-container">
      <div class="search-bar-container">
        <input type="search" id="search-bar" placeholder="Ask me anything">
        <img src="/search-icon.svg" alt="search icon" id="search-icon">
        <img src="/settings-icon.svg" alt="search icon" id="settings-icon">
      </div>
      <div class="settings">
        <label>FOCUS INPUT TO VIEW OPTIONS; BLUR TO CANCEL; ESC TO PAUSE; CLICK <span id="settings-reset-button">[HERE]</span> TO RESET CONFIG</label>
        <!-- Placeholder for dynamically generated settings -->
      </div>
      <div class="cards-container">
        <div class="cards-container-dense">
          <!-- Placeholder for dynamically generated cards -->
        </div>
        <div class="cards-container-multi">
          <!-- Placeholder for dynamically generated cards -->
        </div>
      </div>
    </div>
    <div class="columns-container">
      <!-- Placeholder for dynamically generated columns -->
    </div>
    <div class="errored-page hidden">
      <span>Error connecting to server...</span>
    </div>
  </div>
`

window.onload = async () => {
  (await import("./stomp/stomp.ts")).setupStomp();
  (await import("./search.ts")).setupSearch();
  (await import("./settings.ts")).setupSettings();
  (await import("./columns.ts")).setupColumns();
};
