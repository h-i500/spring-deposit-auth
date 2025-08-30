import Card from "./components/Card";
import LoginPanel from "./features/auth/LoginPanel";
import SavingsSection from "./features/savings/SavingsSection";
import CreateForm from "./features/timeDeposit/CreateForm";
import CloseForm from "./features/timeDeposit/CloseForm";

export default function App() {
  return (
    <main className="container">
      <header className="header">
        <h1>Deposit Demo</h1>
        <p className="sub">
          ログイン/ログアウトと普通預金（Savings）は従来どおり利用可能です。<br />
          定期預金は <strong>「作成」</strong> と <strong>「満期解約」</strong> を分離して提供します。
        </p>
      </header>

      <Card title="認証（ログイン／ログアウト）">
        <LoginPanel />
      </Card>

      <SavingsSection />

      <CreateForm />
      <CloseForm />

      <footer className="footer">
        <small>
          API はセッション Cookie 前提（<code>credentials: "include"</code>）。ベースパスは
          <code> VITE_API_BASE</code> または dev proxy を使用します。
        </small>
      </footer>
    </main>
  );
}
