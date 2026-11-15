// Add Expense, Activity, Profile

function AddExpenseScreen({ app, route }) {
  const [gid, setGid] = React.useState(route.group || 'elm');
  const g = app.groups.find((x) => x.id === gid);
  const [amount, setAmount] = React.useState(route.preset?.amount ?? '86.40');
  const [title, setTitle] = React.useState(route.preset?.title ?? 'Thai takeout');
  const [cat, setCat] = React.useState(route.preset?.cat ?? 'food');
  const [date, setDate] = React.useState('Today');
  const [paidBy, setPaidBy] = React.useState('me');
  const [mode, setMode] = React.useState(route.preset?.mode ?? 'equal');
  const [incl, setIncl] = React.useState(() => Object.fromEntries(g.members.map((m) => [m, true])));
  const [pct, setPct] = React.useState({});
  const [custom, setCustom] = React.useState({});
  const [sheet, setSheet] = React.useState(null);

  React.useEffect(() => {
    setIncl(Object.fromEntries(g.members.map((m) => [m, true])));
    const n = g.members.length;
    const base = Math.floor(100 / n);
    setPct(Object.fromEntries(g.members.map((m, i) => [m, i === 0 ? 100 - base * (n - 1) : base])));
    setCustom({});
    if (!g.members.includes(paidBy)) setPaidBy('me');
  }, [gid]);

  const amt = parseFloat(amount) || 0;
  const parts = g.members.filter((m) => incl[m]);
  const eq = parts.length ? stbEqual(amt, parts) : {};
  const pctTotal = parts.reduce((s, m) => s + (pct[m] || 0), 0);
  const customTotal = r2(parts.reduce((s, m) => s + (parseFloat(custom[m]) || 0), 0));
  const shareOf = (m) => !incl[m] ? 0 : mode === 'equal' ? eq[m] : mode === 'percent' ? r2(amt * (pct[m] || 0) / 100) : (parseFloat(custom[m]) || 0);
  const remaining = mode === 'percent' ? 100 - pctTotal : r2(amt - customTotal);
  const valid = amt > 0 && title.trim() && parts.length && (mode === 'equal' || Math.abs(remaining) < 0.01);

  const save = () => {
    if (!valid) return;
    const shares = {}; parts.forEach((m) => { shares[m] = shareOf(m); });
    app.addExpense({ id: 'n' + Date.now(), group: gid, title: title.trim(), cat, amount: amt, paidBy, date: STB_TODAY, split: mode, shares });
  };

  const catKeys = ['food', 'groceries', 'transport', 'stay', 'utilities', 'fun', 'coffee', 'other'];

  return (
    <div className="screen-in" style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column' }}>
      <TopBar onBack={app.back} title="Add expense" right={<IconBtn name="photo_camera" onClick={() => app.toast('Receipt scanner opened')} />} />
      <div className="scroll" style={{ flex: 1, overflowY: 'auto', padding: '0 16px 24px' }}>
        {/* group selector */}
        <button className="press" onClick={() => setSheet('group')} style={{ display: 'flex', alignItems: 'center', gap: 10, border: 0, background: T.surface, borderRadius: 999, padding: '6px 14px 6px 6px', fontFamily: T.font, cursor: 'pointer', margin: '0 auto', boxShadow: '0 1px 2px rgba(0,0,0,0.04)' }}>
          <GroupTile group={g} size={30} radius={15} />
          <span style={{ fontSize: 14, fontWeight: 700, color: T.ink }}>{g.name}</span>
          <Icon name="expand_more" size={20} color={T.ink2} />
        </button>

        {/* amount */}
        <div style={{ textAlign: 'center', padding: '22px 0 6px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'baseline' }}>
            <span className="num" style={{ fontSize: 34, fontWeight: 700, color: T.ink3, marginRight: 2 }}>$</span>
            <input className="num amt-input big" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value.replace(/[^0-9.]/g, ''))} style={{ width: `${Math.max(amount.length, 1) * 0.6 + 0.2}em` }} />
          </div>
          <input className="title-input" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="What was it for?" />
        </div>

        {/* category */}
        <div className="hscroll" style={{ display: 'flex', gap: 8, overflowX: 'auto', margin: '14px -16px 0', padding: '0 16px' }}>
          {catKeys.map((k) => (
            <button key={k} onClick={() => setCat(k)} className="catbtn" style={{ background: cat === k ? T.ink : T.surface, color: cat === k ? '#fff' : T.ink }}>
              <Icon name={STB_CATS[k].icon} size={22} fill={cat === k ? 1 : 0} />
              <span>{STB_CATS[k].label}</span>
            </button>
          ))}
        </div>

        {/* meta row */}
        <Card pad={6} style={{ marginTop: 16 }}>
          <MetaRow icon="calendar_today" label="Date" value={date} onClick={() => setDate(date === 'Today' ? 'Yesterday' : 'Today')} />
          <MetaRow icon="account_balance_wallet" label="Paid by" divider onClick={() => setSheet('payer')}
            value={<span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}><Avatar id={paidBy} size={24} />{paidBy === 'me' ? 'You' : STB_PEOPLE[paidBy].short}</span>} />
        </Card>

        {/* split */}
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', margin: '24px 4px 10px' }}>
          <div style={{ fontSize: 17, fontWeight: 700 }}>Split</div>
          <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 600 }}>{parts.length} of {g.members.length} people</div>
        </div>
        <Segmented value={mode} onChange={setMode} options={[{ value: 'equal', label: 'Equally', icon: 'drag_handle' }, { value: 'percent', label: 'Percent', icon: 'percent' }, { value: 'custom', label: 'Custom', icon: 'tune' }]} />

        {/* distribution bar */}
        <div style={{ display: 'flex', height: 10, borderRadius: 5, overflow: 'hidden', gap: 2, margin: '16px 2px 6px', background: T.surface2 }}>
          {parts.map((m) => {
            const s = shareOf(m); if (s <= 0) return null;
            return <div key={m} style={{ flex: s, background: `oklch(0.7 0.1 ${STB_PEOPLE[m].hue})`, transition: 'flex .35s' }} />;
          })}
          {mode !== 'equal' && remaining > 0.009 && <div style={{ flex: mode === 'percent' ? amt * remaining / 100 : remaining }} />}
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, fontWeight: 700, padding: '0 2px', marginBottom: 10 }}>
          <span style={{ color: T.ink3 }}>{mode === 'equal' ? `${stbFmt(parts.length ? amt / parts.length : 0)} each` : 'Allocated'}</span>
          {mode !== 'equal' && (
            <span className="num" style={{ color: Math.abs(remaining) < 0.01 ? T.pos : T.neg }}>
              {Math.abs(remaining) < 0.01 ? '✓ Fully split' : remaining > 0 ? `${mode === 'percent' ? remaining + '%' : stbFmt(remaining)} left` : `${mode === 'percent' ? -remaining + '%' : stbFmt(-remaining)} over`}
            </span>
          )}
        </div>

        <Card pad={6}>
          {g.members.map((m, i) => (
            <div key={m} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 10px', margin: '0 4px', borderTop: i ? `1px solid ${T.line}` : 0, opacity: incl[m] ? 1 : 0.5, transition: 'opacity .2s' }}>
              <button onClick={() => setIncl({ ...incl, [m]: !incl[m] })} className={'check' + (incl[m] ? ' on' : '')} aria-label="include">
                {incl[m] && <Icon name="check" size={16} weight={700} />}
              </button>
              <Avatar id={m} size={34} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14.5, fontWeight: 700 }}>{m === 'me' ? 'You' : STB_PEOPLE[m].name}</div>
                {mode === 'percent' && incl[m] && (
                  <input type="range" min="0" max="100" value={pct[m] || 0} onChange={(e) => setPct({ ...pct, [m]: +e.target.value })} className="slider" style={{ '--p': `${pct[m] || 0}%`, '--c': `oklch(0.6 0.11 ${STB_PEOPLE[m].hue})` }} />
                )}
              </div>
              {mode === 'equal' && <Money v={shareOf(m)} size={14.5} color={incl[m] ? T.ink : T.ink3} />}
              {mode === 'percent' && (
                <div style={{ textAlign: 'right' }}>
                  <div className="num" style={{ fontSize: 14.5, fontWeight: 800 }}>{incl[m] ? pct[m] || 0 : 0}%</div>
                  <div className="num" style={{ fontSize: 11.5, color: T.ink3, fontWeight: 600 }}>{stbFmt(shareOf(m))}</div>
                </div>
              )}
              {mode === 'custom' && (
                <div className="field-sm"><span>$</span>
                  <input className="num" disabled={!incl[m]} placeholder="0.00" value={custom[m] ?? ''} onChange={(e) => setCustom({ ...custom, [m]: e.target.value.replace(/[^0-9.]/g, '') })} />
                </div>
              )}
            </div>
          ))}
        </Card>
      </div>
      <div style={{ padding: '12px 16px 16px', borderTop: `1px solid ${T.line}`, background: T.bg }}>
        <button className="btn-primary" disabled={!valid} onClick={save}>
          <Icon name="check" size={22} weight={600} />Save expense · <span className="num">{stbFmt(amt)}</span>
        </button>
      </div>

      <Sheet open={sheet === 'payer'} onClose={() => setSheet(null)}>
        <div style={{ fontSize: 18, fontWeight: 800, marginBottom: 12 }}>Who paid?</div>
        {g.members.map((m) => (
          <button key={m} className="sheet-row" onClick={() => { setPaidBy(m); setSheet(null); }}>
            <Avatar id={m} size={38} /><span style={{ flex: 1, textAlign: 'left' }}>{m === 'me' ? 'You' : STB_PEOPLE[m].name}</span>
            {paidBy === m && <Icon name="check_circle" size={22} fill={1} color={T.pos} />}
          </button>
        ))}
      </Sheet>
      <Sheet open={sheet === 'group'} onClose={() => setSheet(null)}>
        <div style={{ fontSize: 18, fontWeight: 800, marginBottom: 12 }}>Choose group</div>
        {app.groups.map((x) => (
          <button key={x.id} className="sheet-row" onClick={() => { setGid(x.id); setSheet(null); }}>
            <GroupTile group={x} size={38} radius={12} /><span style={{ flex: 1, textAlign: 'left' }}>{x.name}</span>
            {gid === x.id && <Icon name="check_circle" size={22} fill={1} color={T.pos} />}
          </button>
        ))}
      </Sheet>
    </div>
  );
}

function MetaRow({ icon, label, value, divider, onClick }) {
  return (
    <button onClick={onClick} className="press" style={{ display: 'flex', alignItems: 'center', gap: 12, width: 'calc(100% - 8px)', margin: '0 4px', padding: '12px 10px', border: 0, borderTop: divider ? `1px solid ${T.line}` : 0, background: 'transparent', fontFamily: T.font, cursor: 'pointer', color: T.ink }}>
      <Icon name={icon} size={22} color={T.ink2} />
      <span style={{ flex: 1, textAlign: 'left', fontSize: 14.5, fontWeight: 600, color: T.ink2 }}>{label}</span>
      <span style={{ fontSize: 14.5, fontWeight: 700 }}>{value}</span>
      <Icon name="chevron_right" size={20} color={T.ink3} />
    </button>
  );
}

// ── ACTIVITY ──────────────────────────────────────────────
function ActivityScreen({ app }) {
  const [f, setF] = React.useState('all');
  const items = [
    ...app.expenses.map((e) => ({ type: 'expense', date: e.date, key: e.id, e })),
    ...app.settlements.map((s) => ({ type: 'settle', date: s.date, key: s.id, s })),
  ].filter((it) => f === 'all' || (f === 'expenses' && it.type === 'expense') || (f === 'payments' && it.type === 'settle'))
    .sort((a, b) => b.date.localeCompare(a.date) || b.key.localeCompare(a.key));
  const byDay = [];
  items.forEach((it) => {
    const last = byDay[byDay.length - 1];
    if (last && last.date === it.date) last.items.push(it); else byDay.push({ date: it.date, items: [it] });
  });
  const monthSpend = app.expenses.reduce((s, e) => s + (e.shares.me || 0), 0);

  return (
    <>
      <ScreenBody>
        <TopBar title="Activity" right={<><IconBtn name="search" /><IconBtn name="filter_list" /></>} />
        <div style={{ padding: '0 16px 4px' }}>
          <Card pad={18} style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 600 }}>Your share in November</div>
              <div className="num" style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.6, marginTop: 2 }}>{stbFmt(monthSpend)}</div>
            </div>
            <MiniBars app={app} />
          </Card>
        </div>
        <div style={{ display: 'flex', gap: 8, padding: '16px 16px 4px' }}>
          {[['all', 'All'], ['expenses', 'Expenses'], ['payments', 'Payments']].map(([k, l]) => <Chip key={k} active={f === k} onClick={() => setF(k)}>{l}</Chip>)}
        </div>
        <div style={{ padding: '0 16px' }}>
          {byDay.map((d) => (
            <div key={d.date}>
              <div style={{ fontSize: 12, fontWeight: 800, color: T.ink3, letterSpacing: 0.5, textTransform: 'uppercase', padding: '18px 4px 8px' }}>{stbDayLabel(d.date)}</div>
              <div style={{ position: 'relative' }}>
                <div style={{ position: 'absolute', left: 21, top: 22, bottom: 22, width: 2, background: T.line }} />
                {d.items.map((it) => it.type === 'expense' ? <TimelineExpense key={it.key} e={it.e} app={app} /> : <TimelineSettle key={it.key} s={it.s} app={app} />)}
              </div>
            </div>
          ))}
        </div>
      </ScreenBody>
      <Fab onClick={() => app.push({ name: 'add' })} />
    </>
  );
}

function MiniBars({ app }) {
  const weeks = [0, 0, 0];
  app.expenses.forEach((e) => { const d = +e.date.slice(8); const w = d <= 7 ? 0 : d <= 14 ? 1 : 2; weeks[w] += e.shares.me || 0; });
  const max = Math.max(...weeks, 1);
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 6, height: 48 }}>
      {weeks.map((w, i) => (
        <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
          <div className="grow-y" style={{ width: 14, height: Math.max(4, (w / max) * 36), borderRadius: 4, background: i === 2 ? T.ink : T.surface2 }} />
          <span style={{ fontSize: 9.5, fontWeight: 700, color: T.ink3 }}>W{i + 1}</span>
        </div>
      ))}
    </div>
  );
}

function TimelineExpense({ e, app }) {
  const mine = stbMyShare(e);
  const g = app.groups.find((x) => x.id === e.group);
  const payer = e.paidBy === 'me' ? 'You' : STB_PEOPLE[e.paidBy].short;
  return (
    <div className="press" onClick={() => app.push({ name: 'group', id: e.group })} style={{ display: 'flex', gap: 12, padding: '6px 0', position: 'relative', cursor: 'pointer' }}>
      <div style={{ zIndex: 1 }}><CatTile cat={e.cat} tone={mine.kind === 'lent' ? 'pos' : mine.kind === 'owe' ? 'neg' : 'neutral'} /></div>
      <Card pad={14} style={{ flex: 1, minWidth: 0, borderRadius: 18 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 14.5, fontWeight: 700 }}>{e.title}</div>
            <div style={{ fontSize: 12, color: T.ink3, fontWeight: 500, marginTop: 2 }}>{payer} paid <span className="num">{stbFmt(e.amount)}</span> · {g.name}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            {mine.kind !== 'none' ? <Money v={mine.value} sign={false} size={14.5} weight={800} color={mine.kind === 'lent' ? T.pos : T.neg} /> : <span style={{ fontSize: 12, color: T.ink3, fontWeight: 600 }}>—</span>}
            <div style={{ fontSize: 11, color: T.ink3, fontWeight: 600 }}>{mine.kind === 'lent' ? 'you lent' : mine.kind === 'owe' ? 'your share' : 'not involved'}</div>
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10 }}>
          <AvatarStack ids={Object.keys(e.shares)} size={20} max={5} />
          <span style={{ fontSize: 11.5, color: T.ink3, fontWeight: 600 }}>{e.split === 'equal' ? 'Split equally' : e.split === 'percent' ? 'Split by %' : 'Custom split'}</span>
        </div>
      </Card>
    </div>
  );
}

function TimelineSettle({ s, app }) {
  const nm = (id) => id === 'me' ? 'You' : STB_PEOPLE[id].short;
  const g = app.groups.find((x) => x.id === s.group);
  return (
    <div style={{ display: 'flex', gap: 12, padding: '6px 0', position: 'relative' }}>
      <div style={{ zIndex: 1, width: 44, height: 44, borderRadius: 14, background: T.ink, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        <Icon name="handshake" size={21} />
      </div>
      <div style={{ flex: 1, minWidth: 0, padding: '12px 14px', borderRadius: 18, border: `1.5px dashed ${T.line}`, display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14.5, fontWeight: 700 }}>{nm(s.from)} paid {nm(s.to)}</div>
          <div style={{ fontSize: 12, color: T.ink3, fontWeight: 500, marginTop: 2 }}>{s.method} · {g.name}</div>
        </div>
        <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
          <Money v={s.amount} size={14.5} weight={800} />
          <StatusPill status={s.status} />
        </div>
      </div>
    </div>
  );
}

// ── PROFILE ───────────────────────────────────────────────
function ProfileScreen({ app }) {
  const [notif, setNotif] = React.useState(true);
  const [simplify, setSimplify] = React.useState(true);
  const [bio, setBio] = React.useState(false);
  const Row = ({ icon, label, sub, right, divider }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '14px 10px', margin: '0 4px', borderTop: divider ? `1px solid ${T.line}` : 0 }}>
      <div style={{ width: 40, height: 40, borderRadius: 12, background: T.surface2, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Icon name={icon} size={21} /></div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 15, fontWeight: 700 }}>{label}</div>
        {sub && <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 500, marginTop: 1 }}>{sub}</div>}
      </div>
      {right || <Icon name="chevron_right" size={22} color={T.ink3} />}
    </div>
  );
  return (
    <ScreenBody>
      <TopBar title="Profile" right={<IconBtn name="settings" />} />
      <div style={{ textAlign: 'center', padding: '4px 16px 20px' }}>
        <Avatar id="me" size={88} style={{ margin: '0 auto' }} />
        <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.4, marginTop: 12 }}>Maya Chen</div>
        <div style={{ fontSize: 13.5, color: T.ink3, fontWeight: 600, marginTop: 2 }}>maya.chen@mail.com</div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'center', marginTop: 16 }}>
          {[['4', 'Groups'], ['6', 'Friends'], ['5', 'Settled']].map(([n, l]) => (
            <div key={l} style={{ background: T.surface, borderRadius: 18, padding: '10px 18px', minWidth: 64 }}>
              <div className="num" style={{ fontSize: 18, fontWeight: 800 }}>{n}</div>
              <div style={{ fontSize: 11.5, color: T.ink3, fontWeight: 600 }}>{l}</div>
            </div>
          ))}
        </div>
      </div>
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Card pad={6}>
          <Row icon="account_balance" label="Payment methods" sub="Chase •• 4821 · default" />
          <Row icon="currency_exchange" label="Default currency" sub="USD · US Dollar" divider />
          <Row icon="group" label="Friends" sub="6 people" divider />
        </Card>
        <Card pad={6}>
          <Row icon="notifications" label="Payment reminders" sub="Nudge friends weekly" right={<Toggle on={notif} onChange={setNotif} />} />
          <Row icon="auto_awesome" label="Simplify debts" sub="Minimise transfers in groups" divider right={<Toggle on={simplify} onChange={setSimplify} />} />
          <Row icon="fingerprint" label="Biometric lock" divider right={<Toggle on={bio} onChange={setBio} />} />
        </Card>
        <Card pad={6}>
          <Row icon="help" label="Help & support" />
          <Row icon="logout" label="Sign out" divider right={<span />} />
        </Card>
      </div>
    </ScreenBody>
  );
}

Object.assign(window, { AddExpenseScreen, ActivityScreen, ProfileScreen, MetaRow });
