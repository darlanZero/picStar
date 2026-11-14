const artworks = [
  { title: "Sunbound journey", artist: "Mika Rowe", likes: "12.8K", art: 0, ratio: "3/4", tags: ["original", "character"] },
  { title: "The last train to Vega", artist: "Aster Watanabe", likes: "9.6K", art: 1, ratio: "4/5", tags: ["city", "sci-fi"] },
  { title: "Winter guardian", artist: "Yuki Noa", likes: "21.1K", art: 2, ratio: "3/4", tags: ["fantasy", "original"] },
  { title: "Herbarium dreams", artist: "Ema Verde", likes: "8.4K", art: 3, ratio: "4/5", tags: ["botanical", "character"] },
  { title: "Orbiting home", artist: "Rin Sol", likes: "17.2K", art: 4, ratio: "3/4", tags: ["space", "original"] },
  { title: "Maple oath", artist: "Kuro Hana", likes: "14.9K", art: 5, ratio: "4/5", tags: ["fantasy", "character"] },
  { title: "A field of tomorrows", artist: "Mika Rowe", likes: "7.3K", art: 0, ratio: "4/5", tags: ["landscape"] },
  { title: "Neon reverie", artist: "Aster Watanabe", likes: "11.6K", art: 1, ratio: "3/4", tags: ["city"] },
  { title: "First snow, old shrine", artist: "Yuki Noa", likes: "19.7K", art: 2, ratio: "4/5", tags: ["fantasy"] },
  { title: "Notes on quiet growth", artist: "Ema Verde", likes: "6.9K", art: 3, ratio: "3/4", tags: ["botanical"] },
  { title: "Small steps, infinite sky", artist: "Rin Sol", likes: "13.2K", art: 4, ratio: "4/5", tags: ["space"] },
  { title: "Crimson season", artist: "Kuro Hana", likes: "23.0K", art: 5, ratio: "3/4", tags: ["original"] }
];

const artists = [
  { name:"Mika Rowe", handle:"@mika.rowe", art:0, followers:"142K" },
  { name:"Aster Watanabe", handle:"@aster.wav", art:1, followers:"98K" },
  { name:"Yuki Noa", handle:"@yukinoa", art:2, followers:"211K" },
  { name:"Ema Verde", handle:"@emaverde", art:3, followers:"76K" },
  { name:"Rin Sol", handle:"@rinsol", art:4, followers:"185K" },
  { name:"Kuro Hana", handle:"@kurohana", art:5, followers:"309K" }
];

const rankings = ["Daily","For Male","For Female","Original","Rookie","Weekly","Monthly","AI","XVIII AI","XVIII","XVIII weekly","XVIII G"];
const rankingDescriptions = {
  Daily: "The illustrations capturing imaginations today.", Rookie: "Bright new voices finding their first audience.",
  Weekly: "The week’s most resonant visual stories.", Monthly: "A month of art worth remembering.", AI: "Popular AI-assisted illustration, clearly labeled.",
  "For Male": "The current ranking curated for male audiences.", "For Female": "The current ranking curated for female audiences.",
  Original: "Original worlds and characters, ranked by readers."
};

let favorites = new Set([0, 2, 4, 9, 11]);
let feedRound = 0;
const $ = (q, root=document) => root.querySelector(q);
const $$ = (q, root=document) => [...root.querySelectorAll(q)];

function artCard(item, index, options={}) {
  const id = index % artworks.length;
  const favored = favorites.has(id);
  return `<article class="art-card" data-id="${id}">
    <div class="art-media" style="--ratio:${item.ratio}">
      <div class="atlas-art art${item.art}"></div>
      ${options.rank ? `<span class="rank-number">#${String(options.rank).padStart(2,"0")}</span>` : ""}
      ${options.ai ? `<span class="ai-badge">AI</span>` : ""}
      <div class="art-actions"><button class="open-art" aria-label="Abrir ${item.title}">↗</button><button class="bookmark ${favored ? "active" : ""}" data-favorite="${id}" aria-label="Adicionar aos favoritos">${favored ? "♥" : "♡"}</button></div>
    </div>
    <div class="art-meta"><div class="art-copy"><div class="art-title">${item.title}</div><div class="art-author">${item.artist}</div></div><span class="like-count">♡ ${item.likes}</span></div>
  </article>`;
}

function renderGallery(target, items, options={}) {
  $(target).innerHTML = items.map((item, i) => artCard(item, i + (options.offset || 0), { rank: options.ranked ? i+1 : null, ai: options.ai && i%3===0 })).join("");
}

function renderArtists() {
  $("#artist-carousel").innerHTML = artists.slice(0,4).map((a, i) => `<article class="artist-card">
    <div class="artist-head"><span class="atlas-avatar mini-art art${a.art}"></span><div><strong>${a.name}</strong><small>${a.followers} followers</small></div><button class="follow-button ${i < 2 ? "following" : ""}">${i < 2 ? "Following" : "+ Follow"}</button></div>
    <div class="artist-artworks">${[0,1,2,3,4].map((_,n)=>`<span class="mini-art art${(a.art+n)%6}"></span>`).join("")}</div>
  </article>`).join("");
}

function renderFollowingArtists() {
  $("#artist-stream").innerHTML = artists.slice(0,4).map(a => `<section class="stream-group"><div class="stream-head"><span class="atlas-avatar mini-art art${a.art}"></span><div><h3>${a.name}</h3><p>Posted ${a.art + 1} hours ago · ${a.followers} followers</p></div></div><div class="stream-art">${Array.from({length:5},(_,i)=>`<div class="art-media"><div class="atlas-art art${(a.art+i)%6}"></div></div>`).join("")}</div></section>`).join("");
}

function renderRankings(active="Daily") {
  $("#ranking-tabs").innerHTML = rankings.map(r=>`<button class="${r===active?"active":""}" data-ranking="${r}">${r}</button>`).join("");
  $("#ranking-title").textContent = `${active} ranking`;
  $("#ranking-copy").textContent = rankingDescriptions[active] || `A focused look at what is trending in ${active.toLowerCase()}.`;
  const shifted = [...artworks.slice(active.length%6), ...artworks.slice(0,active.length%6)];
  renderGallery("#popular-grid", shifted, { ranked:true, ai:active.includes("AI") });
}

function renderFavorites() {
  const items = artworks.map((a,i)=>({a,i})).filter(x=>favorites.has(x.i));
  $("#favorite-count").textContent = items.length;
  $("#favorites-grid").innerHTML = items.map(x=>artCard(x.a,x.i)).join("");
  $("#favorites-empty").style.display = items.length ? "none" : "block";
}

function goToPage(page) {
  $$(".page").forEach(p=>p.classList.remove("active"));
  $(`#${page}-page`).classList.add("active");
  $$(`[data-page]`).forEach(b=>b.classList.toggle("active",b.dataset.page===page));
  const titles = {home:"Good evening, Luna",popular:"Popular constellations",favorites:"Your saved universe",settings:"Your PicStar"};
  $("#page-title").innerHTML = `${titles[page]} <span>✦</span>`;
  window.scrollTo({top:0,behavior:"smooth"});
  if(page==="favorites") renderFavorites();
}

function initSearch() {
  const recent = ["moonlit city", "Mika Rowe", "#botanical", "original character"];
  const tags = ["#bluehour","#fantasy","#spaceart","#original","#landscape","#softlight","#citypop","#flowers"];
  const renderRecent = () => $("#recent-list").innerHTML = recent.length ? recent.map(x=>`<button class="recent-item" data-search="${x}"><span>↺</span><b>${x}</b><em>×</em></button>`).join("") : `<p class="art-author">No recent searches.</p>`;
  renderRecent();
  $("#recommended-tags").innerHTML = tags.map(t=>`<button data-search="${t}">${t}</button>`).join("");
  const overlay=$("#search-overlay"), input=$("#search-input");
  const open=()=>{overlay.classList.add("open");overlay.setAttribute("aria-hidden","false");setTimeout(()=>input.focus(),60)};
  const close=()=>{overlay.classList.remove("open");overlay.setAttribute("aria-hidden","true");input.value="";update()};
  const update=()=>{
    const q=input.value.trim(); $("#search-clear").style.display=q?"block":"none";
    $("#search-default").style.display=q.length>=2?"none":"block";
    $("#search-results").style.display=q.length>=2?"block":"none";
    if(q.length>=2){
      const matches=[...artworks.slice(0,3), {title:`Artists matching “${q}”`,artist:"6 creator profiles",art:5}];
      $("#result-list").innerHTML=matches.map((a,i)=>`<div class="result-item"><span class="result-thumb art${a.art}"></span><span><strong>${a.title}</strong><small>${a.artist}</small></span></div>`).join("");
      $(".all-results span").textContent=q;
    }
  };
  $("#search-open").addEventListener("click",open); $("#search-close").addEventListener("click",close);
  $("#search-clear").addEventListener("click",()=>{input.value="";update();input.focus()});
  $("#clear-history").addEventListener("click",()=>{recent.length=0;renderRecent()});
  input.addEventListener("input",update);
  overlay.addEventListener("click",e=>{const trigger=e.target.closest("[data-search]");if(trigger){input.value=trigger.dataset.search;update();input.focus()}});
  document.addEventListener("keydown",e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==="k"){e.preventDefault();open()}if(e.key==="Escape"&&overlay.classList.contains("open"))close()});
}

function initSettings() {
  const prefs=[["Show mature content","Keep sensitive work hidden by default.",false],["Include AI-assisted work","Show clearly labeled AI illustration.",true],["Autoplay carousel","Advance featured artwork automatically.",true],["High-resolution previews","Use more data for sharper imagery.",false]];
  $("#preference-list").innerHTML=prefs.map(p=>`<div class="setting-row"><span><strong>${p[0]}</strong><small>${p[1]}</small></span><button class="toggle ${p[2]?"active":""}" aria-label="Alternar ${p[0]}"></button></div>`).join("");
  $$(".settings-nav button").forEach(b=>b.addEventListener("click",()=>{
    $$(".settings-nav button").forEach(x=>x.classList.toggle("active",x===b));
    $$(".settings-panel").forEach(x=>x.classList.toggle("active",x.id===`settings-${b.dataset.settings}`));
  }));
  $("#preference-list").addEventListener("click",e=>e.target.closest(".toggle")?.classList.toggle("active"));
  $$(".theme-card").forEach(b=>b.addEventListener("click",()=>{
    $$(".theme-card").forEach(x=>x.classList.toggle("active",x===b));
    document.body.classList.toggle("light",b.dataset.theme==="daylight");
  }));
  $(".save-settings").addEventListener("click",()=>{const t=$("#toast");t.classList.add("show");setTimeout(()=>t.classList.remove("show"),2200)});
}

function init() {
  renderArtists();
  renderGallery("#recommended-grid", artworks);
  renderGallery("#following-grid", [...artworks].reverse());
  renderFollowingArtists();
  renderRankings();
  renderFavorites();
  initSearch(); initSettings();

  $$(`[data-page]`).forEach(b=>b.addEventListener("click",e=>{e.preventDefault();goToPage(b.dataset.page)}));
  $$("[data-home-tab]").forEach(b=>b.addEventListener("click",()=>{
    $$("[data-home-tab]").forEach(x=>x.classList.toggle("active",x===b));
    $$(".home-panel").forEach(x=>x.classList.toggle("active",x.id===`${b.dataset.homeTab}-panel`));
  }));
  $$("[data-follow-mode]").forEach(b=>b.addEventListener("click",()=>{
    $$("[data-follow-mode]").forEach(x=>x.classList.toggle("active",x===b));
    $$(".following-view").forEach(x=>x.classList.toggle("active",x.id===`following-${b.dataset.followMode}`));
  }));
  $("#ranking-tabs").addEventListener("click",e=>{const b=e.target.closest("[data-ranking]");if(b)renderRankings(b.dataset.ranking)});
  document.addEventListener("click",e=>{
    const fav=e.target.closest("[data-favorite]");
    if(fav){const id=+fav.dataset.favorite;favorites.has(id)?favorites.delete(id):favorites.add(id);$$(`[data-favorite="${id}"]`).forEach(b=>{b.classList.toggle("active",favorites.has(id));b.textContent=favorites.has(id)?"♥":"♡"});if($("#favorites-page").classList.contains("active"))renderFavorites()}
    const follow=e.target.closest(".follow-button");if(follow){follow.classList.toggle("following");follow.textContent=follow.classList.contains("following")?"Following":"+ Follow"}
  });
  $("#view-artists").addEventListener("click",()=>{const c=$("#artist-carousel");c.innerHTML=artists.map((a,i)=>`<article class="artist-card"><div class="artist-head"><span class="atlas-avatar mini-art art${a.art}"></span><div><strong>${a.name}</strong><small>${a.followers} followers</small></div><button class="follow-button">+ Follow</button></div><div class="artist-artworks">${[0,1,2,3,4].map((_,n)=>`<span class="mini-art art${(a.art+n)%6}"></span>`).join("")}</div></article>`).join("");c.scrollIntoView({behavior:"smooth",block:"center"})});
  $$(".tag").forEach(t=>t.addEventListener("click",()=>{$$(".tag").forEach(x=>x.classList.toggle("active",x===t))}));

  const observer=new IntersectionObserver(entries=>{if(entries[0].isIntersecting&&feedRound<3){feedRound++;const batch=artworks.slice(0,6).map((a,i)=>({...a,title:`${a.title} · ${feedRound+1}`}));$("#recommended-grid").insertAdjacentHTML("beforeend",batch.map((a,i)=>artCard(a,i+feedRound*6)).join(""));if(feedRound===3)$("#load-sentinel").textContent="You're all caught up ✦"}},{rootMargin:"300px"});
  observer.observe($("#load-sentinel"));
}

init();
